"""
Refinement Processor - Post-Processing Pipeline
Localized denoising, texture matching, upscaling, color grading
"""

import cv2
import numpy as np
from typing import Dict, Tuple, Optional
import time


class RefinementProcessor:
    """
    Complete post-processing refinement pipeline
    Eliminates "pasted" effect while preserving identity
    """
    
    def __init__(self, upscale_factor=4, enable_upscale=True):
        """
        Initialize refinement processor
        
        Args:
            upscale_factor: 2 or 4
            enable_upscale: Enable Real-ESRGAN upscaling
        """
        self.upscale_factor = upscale_factor
        self.enable_upscale = enable_upscale
        self.color_grader = ColorGrader()
        
        # Load upscaler if enabled
        if self.enable_upscale:
            self.upsampler = self._load_upsampler()
    
    def process(
        self,
        flux_output: np.ndarray,
        reference_image: np.ndarray,
        prompt: str,
        denoise_strength: float = 0.40
    ) -> Tuple[np.ndarray, Dict]:
        """
        Apply full refinement pipeline
        
        Args:
            flux_output: Image from Flux.1 + PuLID (1024x1024)
            reference_image: Original user photo
            prompt: Master prompt (for scene detection)
            denoise_strength: 0.30-0.45 range (NEVER > 0.50)
        
        Returns:
            refined: Fully refined image
            metrics: Quality metrics
        """
        
        start_time = time.time()
        
        # Validate denoise strength
        if denoise_strength > 0.50:
            print(f"⚠️  Warning: Denoise strength {denoise_strength} > 0.50, clamping to 0.45")
            denoise_strength = 0.45
        
        print("🎨 Starting Refinement Pipeline...")
        
        # Stage 1: Localized Harmonization (2-3s)
        print("  Stage 1/4: Localized Harmonization...")
        edge_mask = self._create_edge_mask(flux_output)
        harmonized = self._localized_denoise(flux_output, edge_mask, denoise_strength)
        harmonized = self._apply_rim_light(harmonized, flux_output, edge_mask)
        
        # Stage 2: Texture Matching (1-2s)
        print("  Stage 2/4: Texture Matching...")
        ref_profile = self._analyze_grain(reference_image)
        face_mask = self._extract_face_region(reference_image)
        textured = self._match_grain(harmonized, ref_profile, face_mask)
        textured = self._match_sharpness(textured, reference_image, face_mask)
        
        # Stage 3: Super-Resolution (3-4s)
        if self.enable_upscale:
            print("  Stage 3/4: Super-Resolution (4x upscale)...")
            upscaled = self._upscale_image(textured)
        else:
            print("  Stage 3/4: Super-Resolution (skipped)...")
            upscaled = textured
        
        # Stage 4: Color Grading (1s)
        print("  Stage 4/4: Color Grading...")
        scene_type = self._detect_scene_type(prompt)
        graded = self.color_grader.apply_lut(upscaled, scene_type)
        
        # Calculate metrics
        processing_time = time.time() - start_time
        metrics = {
            "processing_time": processing_time,
            "denoise_strength_used": denoise_strength,
            "upscale_factor": self.upscale_factor if self.enable_upscale else 1,
            "scene_type": scene_type,
            "output_resolution": f"{graded.shape[1]}x{graded.shape[0]}"
        }
        
        print(f"✅ Refinement Complete in {processing_time:.2f}s")
        
        return graded, metrics
    
    def _create_edge_mask(self, image: np.ndarray) -> np.ndarray:
        """Create mask for selective denoising at edges"""
        
        # Convert to grayscale
        gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
        
        # Detect edges
        edges = cv2.Canny(gray, threshold1=50, threshold2=150)
        
        # Dilate to create boundary region
        kernel = np.ones((15, 15), np.uint8)
        dilated = cv2.dilate(edges, kernel, iterations=2)
        
        # Gaussian blur for smooth transition
        edge_mask = cv2.GaussianBlur(dilated, (21, 21), 0)
        
        # Normalize to 0-1
        edge_mask = edge_mask.astype(float) / 255.0
        
        return edge_mask
    
    def _localized_denoise(
        self,
        image: np.ndarray,
        edge_mask: np.ndarray,
        strength: float
    ) -> np.ndarray:
        """
        Apply denoising only to edge regions
        Simulates img2img at edges without full API call
        """
        
        # Use bilateral filter for edge smoothing
        denoised = cv2.bilateralFilter(
            image,
            d=9,
            sigmaColor=int(strength * 150),
            sigmaSpace=int(strength * 150)
        )
        
        # Blend original and denoised using edge mask
        harmonized = (
            image * (1 - edge_mask[:, :, np.newaxis]) +
            denoised * edge_mask[:, :, np.newaxis]
        )
        
        return harmonized.astype(np.uint8)
    
    def _apply_rim_light(
        self,
        image: np.ndarray,
        background: np.ndarray,
        edge_mask: np.ndarray,
        intensity: float = 0.3
    ) -> np.ndarray:
        """Apply rim lighting based on background illumination"""
        
        # Calculate dominant background color
        bg_color = np.mean(background, axis=(0, 1))
        
        # Create rim light layer
        rim_light = np.ones_like(image) * bg_color
        
        # Apply only at edges with intensity
        rim_mask = edge_mask * intensity
        
        # Additive blending
        lit_image = image.astype(float) + (rim_light * rim_mask[:, :, np.newaxis])
        
        # Clip to valid range
        lit_image = np.clip(lit_image, 0, 255)
        
        return lit_image.astype(np.uint8)
    
    def _analyze_grain(self, image: np.ndarray) -> Dict:
        """Analyze image grain/noise characteristics"""
        
        # Convert to grayscale
        gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
        
        # High-pass filter to isolate grain
        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        grain = cv2.subtract(gray, blurred)
        
        # Statistics
        grain_profile = {
            "std": float(np.std(grain)),
            "mean": float(np.mean(grain)),
        }
        
        return grain_profile
    
    def _extract_face_region(self, image: np.ndarray) -> np.ndarray:
        """Extract face region mask (simplified)"""
        
        # For production, use face detection
        # Here we use center region as approximation
        h, w = image.shape[:2]
        mask = np.zeros((h, w), dtype=float)
        
        # Center region (60% of image)
        y1, y2 = int(h * 0.2), int(h * 0.8)
        x1, x2 = int(w * 0.2), int(w * 0.8)
        mask[y1:y2, x1:x2] = 1.0
        
        # Gaussian blur for smooth edges
        mask = cv2.GaussianBlur(mask, (51, 51), 0)
        
        return mask
    
    def _match_grain(
        self,
        generated: np.ndarray,
        reference_profile: Dict,
        face_region: np.ndarray
    ) -> np.ndarray:
        """Match generated image grain to reference"""
        
        # Analyze current grain
        current_profile = self._analyze_grain(generated)
        
        # Calculate adjustment needed
        grain_diff = reference_profile["std"] - current_profile["std"]
        
        # Add slight grain if needed
        if abs(grain_diff) > 2:
            noise = np.random.normal(0, abs(grain_diff), generated.shape)
            adjusted = generated.astype(float) + (noise * face_region[:, :, np.newaxis] * 0.5)
            adjusted = np.clip(adjusted, 0, 255)
            return adjusted.astype(np.uint8)
        
        return generated
    
    def _match_sharpness(
        self,
        generated: np.ndarray,
        reference: np.ndarray,
        face_region: np.ndarray
    ) -> np.ndarray:
        """Match face sharpness to reference photo"""
        
        # Measure sharpness (Laplacian variance)
        ref_gray = cv2.cvtColor(reference, cv2.COLOR_RGB2GRAY)
        gen_gray = cv2.cvtColor(generated, cv2.COLOR_RGB2GRAY)
        
        ref_sharpness = cv2.Laplacian(ref_gray, cv2.CV_64F).var()
        gen_sharpness = cv2.Laplacian(gen_gray, cv2.CV_64F).var()
        
        sharpness_ratio = ref_sharpness / (gen_sharpness + 1e-6)
        
        if sharpness_ratio > 1.15:
            # Generated too soft, sharpen slightly
            kernel = np.array([[-1,-1,-1],
                              [-1, 9,-1],
                              [-1,-1,-1]]) * 0.1
            sharpened = cv2.filter2D(generated, -1, kernel)
            
            # Apply only to face
            result = (
                generated * (1 - face_region[:, :, np.newaxis]) +
                sharpened * face_region[:, :, np.newaxis]
            )
            return result.astype(np.uint8)
        
        return generated
    
    def _upscale_image(self, image: np.ndarray) -> np.ndarray:
        """
        Upscale using Real-ESRGAN
        Note: Requires Real-ESRGAN model files
        """
        
        try:
            # For production, use Real-ESRGAN
            # Here we use high-quality bicubic as fallback
            h, w = image.shape[:2]
            new_h, new_w = h * self.upscale_factor, w * self.upscale_factor
            
            upscaled = cv2.resize(
                image,
                (new_w, new_h),
                interpolation=cv2.INTER_CUBIC
            )
            
            # Apply unsharp mask for clarity
            gaussian = cv2.GaussianBlur(upscaled, (0, 0), 2.0)
            upscaled = cv2.addWeighted(upscaled, 1.5, gaussian, -0.5, 0)
            
            return np.clip(upscaled, 0, 255).astype(np.uint8)
            
        except Exception as e:
            print(f"⚠️  Upscaling failed: {e}, returning original")
            return image
    
    def _detect_scene_type(self, prompt: str) -> str:
        """Auto-detect scene type from prompt"""
        
        prompt_lower = prompt.lower()
        
        if any(word in prompt_lower for word in ["mars", "desert", "sunset", "warm"]):
            return "mars"
        elif any(word in prompt_lower for word in ["night", "evening", "moonlight", "neon"]):
            return "night"
        elif any(word in prompt_lower for word in ["vintage", "retro", "film", "1920"]):
            return "vintage"
        else:
            return "neutral"
    
    def _load_upsampler(self):
        """Load Real-ESRGAN model (placeholder)"""
        # In production, load actual model
        return None


class ColorGrader:
    """Apply scene-aware color grading using LUTs"""
    
    # Predefined LUTs for common scenes
    LUTS = {
        "mars": np.array([  # Warm orange tint
            [1.15, 0.95, 0.80],  # Highlights
            [1.10, 1.00, 0.85],  # Midtones
            [1.05, 1.05, 0.90]   # Shadows
        ]),
        "night": np.array([  # Blue cool tint
            [0.80, 0.85, 1.10],
            [0.85, 0.90, 1.15],
            [0.90, 0.95, 1.20]
        ]),
        "vintage": np.array([  # Faded, warm
            [1.05, 1.00, 0.95],
            [1.00, 0.95, 0.90],
            [0.95, 0.90, 0.85]
        ]),
        "neutral": np.array([  # No change
            [1.0, 1.0, 1.0],
            [1.0, 1.0, 1.0],
            [1.0, 1.0, 1.0]
        ])
    }
    
    def apply_lut(self, image: np.ndarray, scene_type: str = "neutral") -> np.ndarray:
        """
        Apply color grading LUT
        
        Args:
            image: Input image
            scene_type: Scene type (mars, night, vintage, neutral)
        
        Returns:
            graded: Color-graded image
        """
        
        lut = self.LUTS.get(scene_type, self.LUTS["neutral"])
        
        # Normalize image to 0-1
        img = image.astype(float) / 255.0
        
        # Separate luminance zones
        luminance = np.mean(img, axis=2)
        
        highlights = (luminance > 0.66).astype(float)[:, :, np.newaxis]
        midtones = ((luminance >= 0.33) & (luminance <= 0.66)).astype(float)[:, :, np.newaxis]
        shadows = (luminance < 0.33).astype(float)[:, :, np.newaxis]
        
        # Apply LUT
        graded = (
            img * lut[0] * highlights +
            img * lut[1] * midtones +
            img * lut[2] * shadows
        )
        
        # Normalize masks
        total_mask = highlights + midtones + shadows
        graded = graded / (total_mask + 1e-6)
        
        # Convert back to 0-255
        graded = np.clip(graded * 255, 0, 255).astype(np.uint8)
        
        return graded
