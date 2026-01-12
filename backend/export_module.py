"""
Export Module - Format Optimization, Watermarking, Metadata
Android-optimized image export with ethical AI compliance
"""

import cv2
import numpy as np
from PIL import Image
from PIL.ExifTags import TAGS
import piexif
from datetime import datetime
from typing import Dict, Optional, Tuple
import io


class ImageExporter:
    """
    Export final image with watermarking, metadata, and optimization
    """
    
    def __init__(self):
        self.supported_formats = ["jpeg", "heif", "webp", "png"]
    
    def export(
        self,
        image: np.ndarray,
        output_format: str = "webp",
        quality: int = 95,
        add_watermark: bool = True,
        metadata: Optional[Dict] = None
    ) -> Tuple[bytes, str]:
        """
        Export image with all optimizations
        
        Args:
            image: Final refined image
            output_format: jpeg, heif, webp, png
            quality: 1-100
            add_watermark: Add SynthID watermark
            metadata: Custom metadata to embed
        
        Returns:
            image_bytes: Encoded image
            filename: Suggested filename
        """
        
        print(f"📤 Exporting as {output_format.upper()}...")
        
        # Convert numpy to PIL
        pil_image = Image.fromarray(cv2.cvtColor(image, cv2.COLOR_RGB2BGR))
        
        # Add invisible watermark (SynthID)
        if add_watermark:
            pil_image = self._add_synthid_watermark(pil_image)
        
        # Embed metadata
        if metadata:
            pil_image = self._embed_metadata(pil_image, metadata)
        
        # Encode based on format
        image_bytes = self._encode_image(pil_image, output_format, quality)
        
        # Generate filename
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"identitylens_{timestamp}.{output_format}"
        
        print(f"✅ Export Complete: {len(image_bytes) / 1024 / 1024:.2f} MB")
        
        return image_bytes, filename
    
    def _add_synthid_watermark(self, image: Image.Image) -> Image.Image:
        """
        Add invisible SynthID watermark
        
        Note: This is a placeholder for actual SynthID implementation
        Google SynthID requires proprietary access
        """
        
        print("  🔐 Adding SynthID watermark...")
        
        # Placeholder: Add subtle pattern in least significant bits
        # In production, use actual SynthID library
        
        img_array = np.array(image)
        
        # Create watermark pattern
        h, w = img_array.shape[:2]
        watermark_key = "IdentityLens-AI-Generated"
        
        # Convert text to binary
        watermark_binary = ''.join(format(ord(c), '08b') for c in watermark_key)
        
        # Embed in LSB of blue channel (least visible)
        watermark_idx = 0
        for i in range(min(len(watermark_binary), h * w)):
            if watermark_idx >= len(watermark_binary):
                break
            
            y = i // w
            x = i % w
            
            # Modify LSB of blue channel
            if watermark_idx < len(watermark_binary):
                img_array[y, x, 2] = (img_array[y, x, 2] & 0xFE) | int(watermark_binary[watermark_idx])
                watermark_idx += 1
        
        watermarked_image = Image.fromarray(img_array)
        
        print("  ✓ Watermark embedded")
        
        return watermarked_image
    
    def _embed_metadata(self, image: Image.Image, metadata: Dict) -> Image.Image:
        """
        Embed EXIF metadata
        
        Args:
            image: PIL Image
            metadata: Dict with keys: model, prompt, timestamp, etc.
        
        Returns:
            image_with_metadata: Image with EXIF data
        """
        
        print("  📝 Embedding metadata...")
        
        # Prepare EXIF data
        exif_dict = {
            "0th": {},
            "Exif": {},
            "GPS": {},
            "1st": {},
            "thumbnail": None
        }
        
        # Software tag
        exif_dict["0th"][piexif.ImageIFD.Software] = b"IdentityLens AI"
        
        # Model used
        if "model" in metadata:
            exif_dict["0th"][piexif.ImageIFD.Model] = metadata["model"].encode()
        
        # Artist/Creator
        exif_dict["0th"][piexif.ImageIFD.Artist] = b"AI Generated - IdentityLens"
        
        # Copyright
        exif_dict["0th"][piexif.ImageIFD.Copyright] = b"AI Generated Image"
        
        # Description (store prompt)
        if "prompt" in metadata:
            exif_dict["0th"][piexif.ImageIFD.ImageDescription] = metadata["prompt"][:200].encode()
        
        # Datetime
        timestamp = datetime.now().strftime("%Y:%m:%d %H:%M:%S")
        exif_dict["0th"][piexif.ImageIFD.DateTime] = timestamp.encode()
        exif_dict["Exif"][piexif.ExifIFD.DateTimeOriginal] = timestamp.encode()
        
        # User comment (JSON metadata)
        import json
        comment = json.dumps({
            "generator": "IdentityLens",
            "model": metadata.get("model", "Flux.1 + PuLID"),
            "version": "1.0",
            "ai_generated": True,
            "processing_time": metadata.get("processing_time", None)
        })
        exif_dict["Exif"][piexif.ExifIFD.UserComment] = comment.encode()
        
        # Encode EXIF
        exif_bytes = piexif.dump(exif_dict)
        
        # Create new image with EXIF
        output = io.BytesIO()
        image.save(output, format="JPEG", exif=exif_bytes)
        output.seek(0)
        
        image_with_metadata = Image.open(output)
        
        print("  ✓ Metadata embedded")
        
        return image_with_metadata
    
    def _encode_image(
        self,
        image: Image.Image,
        format: str,
        quality: int
    ) -> bytes:
        """
        Encode image in specified format
        
        Args:
            image: PIL Image
            format: jpeg, heif, webp, png
            quality: 1-100
        
        Returns:
            image_bytes: Encoded image data
        """
        
        output = io.BytesIO()
        
        if format.lower() == "jpeg":
            image.save(output, format="JPEG", quality=quality, optimize=True)
        
        elif format.lower() == "webp":
            # WebP lossless for high quality, lossy for compression
            if quality >= 90:
                image.save(output, format="WEBP", lossless=True)
            else:
                image.save(output, format="WEBP", quality=quality)
        
        elif format.lower() == "heif":
            # HEIF requires pillow-heif
            try:
                import pillow_heif
                pillow_heif.register_heif_opener()
                image.save(output, format="HEIF", quality=quality)
            except ImportError:
                print("  ⚠️  HEIF not supported, falling back to JPEG")
                image.save(output, format="JPEG", quality=quality, optimize=True)
        
        elif format.lower() == "png":
            # PNG is lossless
            image.save(output, format="PNG", compress_level=6)
        
        else:
            # Default to JPEG
            image.save(output, format="JPEG", quality=quality, optimize=True)
        
        return output.getvalue()
    
    def optimize_for_social_media(
        self,
        image: np.ndarray,
        platform: str = "instagram"
    ) -> np.ndarray:
        """
        Optimize image for social media platforms
        
        Args:
            image: Input image
            platform: instagram, whatsapp, facebook, twitter
        
        Returns:
            optimized: Resized and optimized image
        """
        
        # Platform-specific requirements
        configs = {
            "instagram": {
                "max_size": 1080,
                "aspect_ratio": (4, 5),  # Portrait
                "quality": 95
            },
            "whatsapp": {
                "max_size": 1600,
                "aspect_ratio": None,  # Keep original
                "quality": 85
            },
            "facebook": {
                "max_size": 2048,
                "aspect_ratio": (16, 9),  # Landscape
                "quality": 90
            },
            "twitter": {
                "max_size": 4096,
                "aspect_ratio": (16, 9),
                "quality": 85
            }
        }
        
        config = configs.get(platform, configs["instagram"])
        
        h, w = image.shape[:2]
        
        # Resize to max size while maintaining aspect ratio
        max_dim = max(h, w)
        if max_dim > config["max_size"]:
            scale = config["max_size"] / max_dim
            new_w = int(w * scale)
            new_h = int(h * scale)
            
            optimized = cv2.resize(
                image,
                (new_w, new_h),
                interpolation=cv2.INTER_LANCZOS4
            )
        else:
            optimized = image.copy()
        
        print(f"  ✓ Optimized for {platform}: {new_w}x{new_h}")
        
        return optimized


class AndroidExportHelper:
    """
    Helper class for Android-specific export operations
    """
    
    @staticmethod
    def create_share_intent_data(
        image_bytes: bytes,
        filename: str,
        mime_type: str = "image/webp"
    ) -> Dict:
        """
        Prepare data for Android Share Intent
        
        Args:
            image_bytes: Encoded image
            filename: Filename
            mime_type: MIME type
        
        Returns:
            intent_data: Data structure for Android Intent
        """
        
        import base64
        
        intent_data = {
            "image_base64": base64.b64encode(image_bytes).decode(),
            "filename": filename,
            "mime_type": mime_type,
            "action": "android.intent.action.SEND",
            "type": mime_type,
            "extra_text": "Paylaş via IdentityLens AI 🎨"
        }
        
        return intent_data
    
    @staticmethod
    def prepare_gallery_save(
        image_bytes: bytes,
        filename: str
    ) -> Dict:
        """
        Prepare data for saving to Android Gallery
        
        Args:
            image_bytes: Encoded image
            filename: Filename
        
        Returns:
            save_data: Data for MediaStore insertion
        """
        
        save_data = {
            "filename": filename,
            "display_name": filename,
            "mime_type": "image/webp",
            "relative_path": "Pictures/IdentityLens",
            "date_added": int(datetime.now().timestamp()),
            "is_pending": 0
        }
        
        return save_data


# Example usage
if __name__ == "__main__":
    # Test export
    test_image = np.random.randint(0, 255, (1024, 1024, 3), dtype=np.uint8)
    
    exporter = ImageExporter()
    
    image_bytes, filename = exporter.export(
        test_image,
        output_format="webp",
        quality=95,
        add_watermark=True,
        metadata={
            "model": "Flux.1 + PuLID",
            "prompt": "Test prompt",
            "processing_time": 8.5
        }
    )
    
    print(f"\nExported: {filename}")
    print(f"Size: {len(image_bytes) / 1024:.2f} KB")
    
    # Test social media optimization
    instagram_optimized = exporter.optimize_for_social_media(test_image, "instagram")
    print(f"Instagram size: {instagram_optimized.shape}")
