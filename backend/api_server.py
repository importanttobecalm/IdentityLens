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

app = FastAPI(
    title="IdentityLens Cloud Inference API",
    description="Flux.1 + PuLID identity-preserving image generation",
    version="1.0.0"
)

# CORS for Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure properly in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Flux client
FAL_API_KEY = os.getenv("FAL_API_KEY")
if not FAL_API_KEY:
    raise ValueError("FAL_API_KEY environment variable not set")

flux_client = FluxPuLIDClient(api_key=FAL_API_KEY, provider="fal")


# Request/Response Models
class GenerationRequest(BaseModel):
    identity_packet: Dict[str, Any] = Field(..., description="Identity packet from Android")
    master_prompt: str = Field(..., description="Optimized Flux prompt")
    negative_prompt: str = Field(..., description="Negative prompt")
    mode: str = Field(default="speed", description="Generation mode: speed or quality")
    lighting_params: Optional[Dict[str, Any]] = Field(None, description="Lighting parameters")
    enable_harmonization: bool = Field(default=True, description="Enable harmonization")
    denoising_strength: float = Field(default=0.40, ge=0.35, le=0.45, description="Denoising strength")


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
