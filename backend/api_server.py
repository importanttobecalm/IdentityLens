"""
FastAPI Backend Server for IdentityLens Cloud Inference
Handles Android app requests and proxies to Fal.ai/Replicate
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional, Dict, Any
import os
import uvicorn
from flux_api_client import (
    FluxPuLIDClient,
    GenerationMode,
    InferenceResult,
    ErrorCodes
)
from refinement_processor import RefinementProcessor
from validation_module import IdentityValidator, QualityMetrics, DataPurgeProtocol
from export_module import ImageExporter

# Initialize processors
flux_client = FluxPuLIDClient(api_key=FAL_API_KEY, provider="fal")
refinement_processor = RefinementProcessor(upscale_factor=4, enable_upscale=True)
identity_validator = IdentityValidator(model_name="ArcFace")
image_exporter = ImageExporter()


# Request/Response Models
class GenerationRequest(BaseModel):
    identity_packet: Dict[str, Any] = Field(..., description="Identity packet from Android")
    master_prompt: str = Field(..., description="Optimized Flux prompt")
    negative_prompt: str = Field(..., description="Negative prompt")
    mode: str = Field(default="speed", description="Generation mode: speed or quality")
    lighting_params: Optional[Dict[str, Any]] = Field(None, description="Lighting parameters")
    enable_harmonization: bool = Field(default=True, description="Enable harmonization")
    denoising_strength: float = Field(default=0.40, ge=0.30, le=0.45, description="Denoising strength")
    
    # Refinement options
    enable_refinement: bool = Field(default=True, description="Enable post-processing refinement")
    enable_upscale: bool = Field(default=False, description="Enable 4x upscaling")
    
    # Validation options
    enable_validation: bool = Field(default=True, description="Enable identity validation")
    
    # Export options
    export_format: str = Field(default="webp", description="Output format: jpeg, webp, heif")
    add_watermark: bool = Field(default=True, description="Add SynthID watermark")


class GenerationResponse(BaseModel):
    success: bool
    image_url: Optional[str] = None
    image_base64: Optional[str] = None
    inference_time: float
    model_version: str
    seed: int = -1
    
    # Refinement metrics
    refinement_metrics: Optional[Dict[str, Any]] = None
    
    # Validation results
    validation_results: Optional[Dict[str, Any]] = None
    
    # Quality metrics
    quality_metrics: Optional[Dict[str, Any]] = None
    
    error: Optional[Dict[str, Any]] = None


@app.post("/api/generate", response_model=GenerationResponse)
async def generate_image(request: GenerationRequest):
    """
    Generate identity-preserved image with refinement and validation
    """
    
    try:
        # Convert mode string to enum
        mode = (
            GenerationMode.SPEED if request.mode == "speed"
            else GenerationMode.QUALITY
        )
        
        # Step 1: Generate with Flux + PuLID
        print("🎨 Step 1: Flux.1 + PuLID Generation...")
        result: InferenceResult = flux_client.generate_with_retry(
            identity_packet=request.identity_packet,
            master_prompt=request.master_prompt,
            negative_prompt=request.negative_prompt,
            mode=mode,
            max_retries=3
        )
        
        if not result.success:
            return GenerationResponse(
                success=False,
                inference_time=result.inference_time,
                model_version="",
                error={
                    "code": result.error_code,
                    "message": result.error_message
                }
            )
        
        # Load generated image
        import requests
        import cv2
        import numpy as np
        
        img_response = requests.get(result.image_url)
        img_array = np.asarray(bytearray(img_response.content), dtype=np.uint8)
        flux_output = cv2.imdecode(img_array, cv2.IMREAD_COLOR)
        flux_output = cv2.cvtColor(flux_output, cv2.COLOR_BGR2RGB)
        
        # Load reference image
        ref_b64 = request.identity_packet["image"]["cleanFace"]
        ref_data = base64.b64decode(ref_b64)
        ref_array = np.frombuffer(ref_data, dtype=np.uint8)
        reference_image = cv2.imdecode(ref_array, cv2.IMREAD_COLOR)
        reference_image = cv2.cvtColor(reference_image, cv2.COLOR_BGR2RGB)
        
        # Step 2: Refinement (optional)
        refined_image = flux_output
        refinement_metrics = None
        
        if request.enable_refinement:
            print("✨ Step 2: Post-Processing Refinement...")
            refined_image, refinement_metrics = refinement_processor.process(
                flux_output=flux_output,
                reference_image=reference_image,
                prompt=request.master_prompt,
                denoise_strength=request.denoising_strength
            )
        
        # Step 3: Validation (optional)
        validation_results = None
        
        if request.enable_validation:
            print("🔍 Step 3: Identity Validation...")
            is_valid, validation_results = identity_validator.validate(
                reference_image=reference_image,
                generated_image=refined_image
            )
            
            if not is_valid:
                print(f"⚠️  Low similarity: {validation_results.get('similarity_score', 0):.2%}")
        
        # Step 4: Quality Metrics
        print("📊 Step 4: Quality Analysis...")
        quality_metrics = QualityMetrics.calculate_all(refined_image)
        
        # Step 5: Export
        print("📤 Step 5: Export...")
        export_metadata = {
            "model": result.model_version,
            "prompt": request.master_prompt[:200],
            "processing_time": result.inference_time + (refinement_metrics.get("processing_time", 0) if refinement_metrics else 0)
        }
        
        image_bytes, filename = image_exporter.export(
            image=refined_image,
            output_format=request.export_format,
            quality=95,
            add_watermark=request.add_watermark,
            metadata=export_metadata
        )
        
        # Upload to storage (or return base64)
        import base64
        image_base64 = base64.b64encode(image_bytes).decode()
        
        # Step 6: Data Purge (privacy)
        identity_id = request.identity_packet.get("captureId", "unknown")
        temp_dir = "/tmp/identitylens"
        DataPurgeProtocol.purge_embeddings(temp_dir, identity_id)
        
        # Response
        return GenerationResponse(
            success=True,
            image_url=result.image_url,  # Original Flux output
            image_base64=image_base64,  # Refined output
            inference_time=result.inference_time,
            model_version=result.model_version,
            seed=result.seed,
            refinement_metrics=refinement_metrics,
            validation_results=validation_results,
            quality_metrics=quality_metrics
        )
    
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "code": ErrorCodes.API_ERROR,
                "message": str(e)
            }
        )


import base64


class GenerationResponse(BaseModel):
    success: bool
    image_url: Optional[str] = None
    image_base64: Optional[str] = None
    inference_time: float
    model_version: str
    seed: int = -1
    error: Optional[Dict[str, Any]] = None


class HealthResponse(BaseModel):
    status: str
    version: str
    provider: str


# Endpoints
@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "version": "1.0.0",
        "provider": "fal.ai"
    }


@app.post("/api/generate", response_model=GenerationResponse)
async def generate_image(request: GenerationRequest):
    """
    Generate identity-preserved image
    
    Receives:
    - Identity packet (from Step 1: Image Capture)
    - Master prompt (from Step 2: Prompt Engine)
    - Negative prompt
    - Generation mode
    
    Returns:
    - Generated image URL
    - Inference metadata
    """
    
    try:
        # Convert mode string to enum
        mode = (
            GenerationMode.SPEED if request.mode == "speed"
            else GenerationMode.QUALITY
        )
        
        # Generate with retry
        result: InferenceResult = flux_client.generate_with_retry(
            identity_packet=request.identity_packet,
            master_prompt=request.master_prompt,
            negative_prompt=request.negative_prompt,
            mode=mode,
            max_retries=3
        )
        
        # Build response
        if result.success:
            return GenerationResponse(
                success=True,
                image_url=result.image_url,
                image_base64=result.image_base64,
                inference_time=result.inference_time,
                model_version=result.model_version,
                seed=result.seed
            )
        else:
            # Return error details
            return GenerationResponse(
                success=False,
                inference_time=result.inference_time,
                model_version="",
                error={
                    "code": result.error_code,
                    "message": result.error_message,
                    "action": _get_error_action(result.error_code),
                    "retry": _is_retryable(result.error_code)
                }
            )
    
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "code": ErrorCodes.API_ERROR,
                "message": str(e),
                "action": "Please try again",
                "retry": True
            }
        )


@app.post("/api/batch-generate")
async def batch_generate(requests: list[GenerationRequest], background_tasks: BackgroundTasks):
    """
    Generate multiple images (for future use)
    """
    # TODO: Implement batch generation
    raise HTTPException(status_code=501, detail="Batch generation not yet implemented")


def _get_error_action(error_code: int) -> str:
    """Get user action for error code"""
    actions = {
        ErrorCodes.NO_FACE_DETECTED: "Lütfen yüzünüzün net göründüğü bir fotoğraf çekin",
        ErrorCodes.LOW_QUALITY_IMAGE: "Daha iyi ışıklandırma ile tekrar deneyin",
        ErrorCodes.INFERENCE_TIMEOUT: "Lütfen tekrar deneyin",
        ErrorCodes.API_RATE_LIMIT: "Lütfen birkaç saniye bekleyin",
        ErrorCodes.NSFW_CONTENT: "Lütfen farklı bir sahne deneyin"
    }
    return actions.get(error_code, "Lütfen tekrar deneyin")


def _is_retryable(error_code: int) -> bool:
    """Check if error is retryable"""
    non_retryable = {
        ErrorCodes.NO_FACE_DETECTED,
        ErrorCodes.NSFW_CONTENT
    }
    return error_code not in non_retryable


if __name__ == "__main__":
    # Run server
    port = int(os.getenv("PORT", "8000"))
    
    uvicorn.run(
        "api_server:app",
        host="0.0.0.0",
        port=port,
        reload=True,  # Disable in production
        log_level="info"
    )
