"""
IdentityLens Cloud Inference Pipeline
Flux.1 + PuLID Identity-Preserving Image Generation

API Provider: Fal.ai (recommended for production)
Model: Flux.1 schnell (speed mode) / Flux.1 dev (quality mode)
"""

import requests
import base64
import json
import time
import os
from typing import Dict, Optional, List
from dataclasses import dataclass, asdict
from enum import Enum


class GenerationMode(Enum):
    """Generation mode selection"""
    SPEED = "speed"      # Flux schnell - 4-6s inference
    QUALITY = "quality"  # Flux dev - 8-10s inference


@dataclass
class InferenceResult:
    """Result from cloud inference"""
    success: bool
    image_url: Optional[str] = None
    image_base64: Optional[str] = None
    inference_time: float = 0.0
    model_version: str = ""
    seed: int = -1
    error_code: Optional[int] = None
    error_message: Optional[str] = None


class ErrorCodes:
    """Error code definitions"""
    NO_FACE_DETECTED = 1001
    LOW_QUALITY_IMAGE = 1002
    INFERENCE_TIMEOUT = 2001
    API_RATE_LIMIT = 2002
    NSFW_CONTENT = 3001
    API_ERROR = 4001
    UNKNOWN_ERROR = 9999


ERROR_MESSAGES = {
    ErrorCodes.NO_FACE_DETECTED: {
        "tr": "Referans görüntüde yüz algılanamadı",
        "en": "No face detected in reference image",
        "action": "Lütfen yüzünüzün net göründüğü bir fotoğraf çekin",
        "retry": False
    },
    ErrorCodes.LOW_QUALITY_IMAGE: {
        "tr": "Görüntü kalitesi yetersiz",
        "en": "Image quality insufficient",
        "action": "Daha iyi ışıklandırma ile tekrar deneyin",
        "retry": True
    },
    ErrorCodes.INFERENCE_TIMEOUT: {
        "tr": "İşlem zaman aşımına uğradı",
        "en": "Inference timeout",
        "action": "Lütfen tekrar deneyin",
        "retry": True
    },
    ErrorCodes.API_RATE_LIMIT: {
        "tr": "İstek limiti aşıldı",
        "en": "API rate limit exceeded",
        "action": "Lütfen birkaç saniye bekleyin",
        "retry": True
    },
    ErrorCodes.NSFW_CONTENT: {
        "tr": "İçerik güvenlik kontrolünden geçemedi",
        "en": "Content failed safety check",
        "action": "Lütfen farklı bir sahne deneyin",
        "retry": False
    }
}


class FluxPuLIDClient:
    """
    Cloud API client for Flux.1 + PuLID identity-preserving image generation
    
    Supports:
    - Fal.ai (recommended)
    - Replicate (alternative)
    - ComfyUI (self-hosted)
    """
    
    def __init__(
        self,
        api_key: str,
        provider: str = "fal",  # fal | replicate | comfyui
        model: str = "flux-schnell"
    ):
        """
        Initialize client
        
        Args:
            api_key: API key for the provider
            provider: API provider (fal, replicate, comfyui)
            model: Base model (flux-schnell or flux-dev)
        """
        self.api_key = api_key
        self.provider = provider
        self.model = model
        
        # Provider endpoints
        self.endpoints = {
            "fal": "https://fal.run/fal-ai/flux-pro/v1.1-ultra",
            "replicate": "https://api.replicate.com/v1/predictions",
            "comfyui": os.getenv("COMFYUI_ENDPOINT", "http://localhost:8188")
        }
        
    def generate_image(
        self,
        identity_packet: Dict,
        master_prompt: str,
        negative_prompt: str,
        mode: GenerationMode = GenerationMode.SPEED,
        lighting_params: Optional[Dict] = None
    ) -> InferenceResult:
        """
        Generate identity-preserved image
        
        Args:
            identity_packet: JSON from Android app (Step 1)
            master_prompt: Optimized prompt from Prompt Engine (Step 2)
            negative_prompt: Context-aware negatives
            mode: Generation mode (speed or quality)
            lighting_params: Optional lighting parameters
        
        Returns:
            InferenceResult with image and metadata
        """
        
        start_time = time.time()
        
        try:
            # Validate inputs
            if not self._validate_identity_packet(identity_packet):
                return InferenceResult(
                    success=False,
                    error_code=ErrorCodes.NO_FACE_DETECTED,
                    error_message=ERROR_MESSAGES[ErrorCodes.NO_FACE_DETECTED]["tr"]
                )
            
            # Build request based on provider
            if self.provider == "fal":
                result = self._generate_fal(
                    identity_packet, master_prompt, negative_prompt, mode
                )
            elif self.provider == "replicate":
                result = self._generate_replicate(
                    identity_packet, master_prompt, negative_prompt, mode
                )
            else:
                raise ValueError(f"Unsupported provider: {self.provider}")
            
            # Apply harmonization if successful
            if result.success and result.image_url:
                result = self._apply_harmonization(result, mode)
            
            result.inference_time = time.time() - start_time
            return result
            
        except requests.Timeout:
            return InferenceResult(
                success=False,
                error_code=ErrorCodes.INFERENCE_TIMEOUT,
                error_message=ERROR_MESSAGES[ErrorCodes.INFERENCE_TIMEOUT]["tr"],
                inference_time=time.time() - start_time
            )
        except Exception as e:
            return InferenceResult(
                success=False,
                error_code=ErrorCodes.API_ERROR,
                error_message=str(e),
                inference_time=time.time() - start_time
            )
    
    def _generate_fal(
        self,
        identity_packet: Dict,
        master_prompt: str,
        negative_prompt: str,
        mode: GenerationMode
    ) -> InferenceResult:
        """Generate using Fal.ai API"""
        
        config = self._get_fal_config(mode)
        
        # Extract face image
        face_image_b64 = identity_packet["image"]["cleanFace"]
        
        # Build request
        request_data = {
            "prompt": master_prompt,
            "negative_prompt": negative_prompt,
            
            # PuLID identity preservation
            "image_prompts": [{
                "image_url": f"data:image/jpeg;base64,{face_image_b64}",
                "weight": 0.85  # Fidelity weight
            }],
            
            # Model configuration
            "model_name": config["model"],
            "num_inference_steps": config["steps"],
            "guidance_scale": config["guidance"],
            
            # Output configuration
            "image_size": {
                "width": 1024,
                "height": 1024
            },
            "num_images": 1,
            "seed": -1,
            
            # Safety and optimization
            "enable_safety_checker": False,
            "sync_mode": True,
            "output_format": "jpeg",
            "jpeg_quality": 90
        }
        
        # Make request
        headers = {
            "Authorization": f"Key {self.api_key}",
            "Content-Type": "application/json"
        }
        
        response = requests.post(
            self.endpoints["fal"],
            headers=headers,
            json=request_data,
            timeout=30
        )
        
        response.raise_for_status()
        data = response.json()
        
        return InferenceResult(
            success=True,
            image_url=data["images"][0]["url"],
            model_version=config["model"],
            seed=data.get("seed", -1)
        )
    
    def _generate_replicate(
        self,
        identity_packet: Dict,
        master_prompt: str,
        negative_prompt: str,
        mode: GenerationMode
    ) -> InferenceResult:
        """Generate using Replicate API"""
        
        config = self._get_replicate_config(mode)
        face_image_b64 = identity_packet["image"]["cleanFace"]
        
        # Build request
        request_data = {
            "version": config["version_id"],
            "input": {
                "prompt": master_prompt,
                "negative_prompt": negative_prompt,
                "image": f"data:image/jpeg;base64,{face_image_b64}",
                "num_inference_steps": config["steps"],
                "guidance_scale": config["guidance"],
                "width": 1024,
                "height": 1024,
                "output_format": "jpg",
                "output_quality": 90
            }
        }
        
        headers = {
            "Authorization": f"Token {self.api_key}",
            "Content-Type": "application/json"
        }
        
        # Start prediction
        response = requests.post(
            self.endpoints["replicate"],
            headers=headers,
            json=request_data,
            timeout=30
        )
        
        response.raise_for_status()
        prediction = response.json()
        
        # Poll for completion
        prediction_url = prediction["urls"]["get"]
        result = self._poll_replicate_prediction(prediction_url, headers)
        
        return result
    
    def _poll_replicate_prediction(
        self,
        prediction_url: str,
        headers: Dict,
        max_wait: int = 60
    ) -> InferenceResult:
        """Poll Replicate prediction until completion"""
        
        start_time = time.time()
        
        while time.time() - start_time < max_wait:
            response = requests.get(prediction_url, headers=headers)
            response.raise_for_status()
            data = response.json()
            
            status = data["status"]
            
            if status == "succeeded":
                return InferenceResult(
                    success=True,
                    image_url=data["output"][0],
                    model_version="flux-schnell"
                )
            elif status == "failed":
                return InferenceResult(
                    success=False,
                    error_code=ErrorCodes.API_ERROR,
                    error_message=data.get("error", "Prediction failed")
                )
            
            time.sleep(1)
        
        return InferenceResult(
            success=False,
            error_code=ErrorCodes.INFERENCE_TIMEOUT,
            error_message="Prediction timeout"
        )
    
    def _apply_harmonization(
        self,
        result: InferenceResult,
        mode: GenerationMode,
        denoising_strength: float = 0.40
    ) -> InferenceResult:
        """
        Apply image-to-image harmonization to prevent uncanny valley
        
        Critical: Denoising strength must be 0.35-0.45 to preserve skin texture
        """
        
        if not result.image_url:
            return result
        
        config = self._get_fal_config(mode)
        
        harmonize_request = {
            "prompt": "professional photography, photorealistic, natural skin texture, detailed pores",
            "image_url": result.image_url,
            "strength": denoising_strength,  # 0.35-0.45 range
            "num_inference_steps": config["steps"],
            "guidance_scale": config["guidance"]
        }
        
        try:
            headers = {
                "Authorization": f"Key {self.api_key}",
                "Content-Type": "application/json"
            }
            
            response = requests.post(
                f"{self.endpoints['fal']}/img2img",
                headers=headers,
                json=harmonize_request,
                timeout=20
            )
            
            if response.status_code == 200:
                data = response.json()
                result.image_url = data["images"][0]["url"]
        
        except Exception as e:
            print(f"Harmonization failed: {e}, using original")
        
        return result
    
    def _validate_identity_packet(self, packet: Dict) -> bool:
        """Validate identity packet has required fields"""
        
        if not packet.get("image", {}).get("cleanFace"):
            return False
        
        if not packet.get("facialData", {}).get("boundingBox"):
            return False
        
        quality_metrics = packet.get("qualityMetrics", {})
        if not quality_metrics.get("passed", False):
            return False
        
        return True
    
    def _get_fal_config(self, mode: GenerationMode) -> Dict:
        """Get Fal.ai configuration based on mode"""
        
        configs = {
            GenerationMode.SPEED: {
                "model": "flux-schnell",
                "steps": 4,
                "guidance": 3.5
            },
            GenerationMode.QUALITY: {
                "model": "flux-dev",
                "steps": 30,
                "guidance": 7.5
            }
        }
        
        return configs[mode]
    
    def _get_replicate_config(self, mode: GenerationMode) -> Dict:
        """Get Replicate configuration based on mode"""
        
        configs = {
            GenerationMode.SPEED: {
                "version_id": "black-forest-labs/flux-schnell",
                "steps": 4,
                "guidance": 3.5
            },
            GenerationMode.QUALITY: {
                "version_id": "black-forest-labs/flux-dev",
                "steps": 50,
                "guidance": 7.5
            }
        }
        
        return configs[mode]
    
    def generate_with_retry(
        self,
        identity_packet: Dict,
        master_prompt: str,
        negative_prompt: str,
        mode: GenerationMode = GenerationMode.SPEED,
        max_retries: int = 3
    ) -> InferenceResult:
        """Generate with automatic retry on transient failures"""
        
        for attempt in range(max_retries):
            result = self.generate_image(
                identity_packet,
                master_prompt,
                negative_prompt,
                mode
            )
            
            if result.success:
                return result
            
            # Check if error is retryable
            if result.error_code in ERROR_MESSAGES:
                error_info = ERROR_MESSAGES[result.error_code]
                if not error_info.get("retry", False):
                    return result
            
            if attempt < max_retries - 1:
                wait_time = 2 ** attempt  # Exponential backoff
                print(f"Attempt {attempt + 1} failed, retrying in {wait_time}s...")
                time.sleep(wait_time)
        
        return result


# Helper function for quick usage
def generate_identity_image(
    api_key: str,
    identity_packet: Dict,
    master_prompt: str,
    negative_prompt: str,
    mode: str = "speed"
) -> InferenceResult:
    """
    Quick helper function for image generation
    
    Args:
        api_key: Fal.ai API key
        identity_packet: Identity packet from Android
        master_prompt: Optimized prompt from Prompt Engine
        negative_prompt: Negative prompt
        mode: "speed" or "quality"
    
    Returns:
        InferenceResult
    """
    
    client = FluxPuLIDClient(api_key=api_key)
    
    generation_mode = (
        GenerationMode.SPEED if mode == "speed" 
        else GenerationMode.QUALITY
    )
    
    return client.generate_with_retry(
        identity_packet=identity_packet,
        master_prompt=master_prompt,
        negative_prompt=negative_prompt,
        mode=generation_mode
    )


if __name__ == "__main__":
    # Example usage
    import sys
    
    # Load test data
    with open("test_identity_packet.json", "r") as f:
        identity_packet = json.load(f)
    
    master_prompt = "A person with exact facial features from reference, standing on rainy 1920s Paris street, wearing vintage suit..."
    negative_prompt = "deformed, bad anatomy, modern clothing..."
    
    # Generate
    api_key = os.getenv("FAL_API_KEY")
    if not api_key:
        print("Error: FAL_API_KEY environment variable not set")
        sys.exit(1)
    
    result = generate_identity_image(
        api_key=api_key,
        identity_packet=identity_packet,
        master_prompt=master_prompt,
        negative_prompt=negative_prompt,
        mode="speed"
    )
    
    if result.success:
        print(f"✅ Success! Image URL: {result.image_url}")
        print(f"⏱️ Inference time: {result.inference_time:.2f}s")
    else:
        print(f"❌ Failed: {result.error_message}")
