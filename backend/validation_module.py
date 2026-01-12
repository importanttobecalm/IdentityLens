"""
Validation Module - Identity Verification & Quality Checks
Face similarity, quality metrics, ethical AI compliance
"""

import cv2
import numpy as np
from typing import Dict, Tuple, Optional
from deepface import DeepFace
import os


class IdentityValidator:
    """
    Verify identity preservation between reference and generated images
    """
    
    SIMILARITY_THRESHOLD = 0.80  # 80% minimum similarity
    
    def __init__(self, model_name="ArcFace"):
        """
        Initialize validator
        
        Args:
            model_name: Face recognition model (ArcFace, Facenet, VGG-Face)
        """
        self.model_name = model_name
    
    def validate(
        self,
        reference_image: np.ndarray,
        generated_image: np.ndarray
    ) -> Tuple[bool, Dict]:
        """
        Validate identity preservation
        
        Args:
            reference_image: Original user photo
            generated_image: AI-generated image
        
        Returns:
            is_valid: True if similarity > threshold
            metrics: Validation metrics
        """
        
        print("🔍 Validating Identity Preservation...")
        
        try:
            # Calculate face similarity
            similarity_score = self._calculate_similarity(
                reference_image,
                generated_image
            )
            
            # Check threshold
            is_valid = similarity_score >= self.SIMILARITY_THRESHOLD
            
            # Prepare metrics
            metrics = {
                "similarity_score": similarity_score,
                "threshold": self.SIMILARITY_THRESHOLD,
                "is_valid": is_valid,
                "model": self.model_name,
                "recommendation": self._get_recommendation(similarity_score)
            }
            
            if is_valid:
                print(f"✅ Identity Preserved: {similarity_score:.2%}")
            else:
                print(f"⚠️  Low Similarity: {similarity_score:.2%} < {self.SIMILARITY_THRESHOLD:.2%}")
            
            return is_valid, metrics
            
        except Exception as e:
            print(f"❌ Validation Error: {e}")
            return False, {
                "error": str(e),
                "is_valid": False
            }
    
    def _calculate_similarity(
        self,
        image1: np.ndarray,
        image2: np.ndarray
    ) -> float:
        """
        Calculate face similarity using DeepFace
        
        Returns:
            similarity: 0-1 score (1 = perfect match)
        """
        
        try:
            # DeepFace verification
            result = DeepFace.verify(
                img1_path=image1,
                img2_path=image2,
                model_name=self.model_name,
                enforce_detection=False,
                detector_backend="opencv"
            )
            
            # Convert distance to similarity
            distance = result["distance"]
            
            # ArcFace distance threshold is ~0.68
            # Convert to 0-1 similarity scale
            if self.model_name == "ArcFace":
                similarity = 1 - (distance / 1.34)  # Normalize
            else:
                similarity = 1 - (distance / 1.5)
            
            similarity = max(0, min(1, similarity))
            
            return similarity
            
        except Exception as e:
            print(f"Face comparison failed: {e}")
            # Fallback to SSIM
            return self._fallback_similarity(image1, image2)
    
    def _fallback_similarity(
        self,
        image1: np.ndarray,
        image2: np.ndarray
    ) -> float:
        """Fallback to SSIM if face detection fails"""
        
        from skimage.metrics import structural_similarity as ssim
        
        # Convert to grayscale
        gray1 = cv2.cvtColor(image1, cv2.COLOR_RGB2GRAY)
        gray2 = cv2.cvtColor(image2, cv2.COLOR_RGB2GRAY)
        
        # Resize to same size
        h, w = gray1.shape
        gray2 = cv2.resize(gray2, (w, h))
        
        # Calculate SSIM
        score = ssim(gray1, gray2)
        
        return score
    
    def _get_recommendation(self, similarity: float) -> str:
        """Get user-friendly recommendation"""
        
        if similarity >= 0.90:
            return "Mükemmel benzerlik! ✨"
        elif similarity >= self.SIMILARITY_THRESHOLD:
            return "İyi benzerlik ✓"
        elif similarity >= 0.70:
            return "Kabul edilebilir - Daha iyi ışıklandırma ile tekrar deneyin 💡"
        else:
            return "Düşük benzerlik - Lütfen daha net bir referans fotoğrafı çekin 📸"


class QualityMetrics:
    """
    Calculate image quality metrics
    """
    
    @staticmethod
    def calculate_brisque(image: np.ndarray) -> float:
        """
        Calculate BRISQUE (no-reference quality)
        
        Returns:
            score: 0-100 (lower = better quality)
        """
        
        try:
            import cv2
            brisque = cv2.quality.QualityBRISQUE_create()
            score = brisque.compute(image)[0]
            return float(score)
        except:
            # Fallback: use variance as quality indicator
            gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
            variance = np.var(gray)
            # Normalize to 0-100 scale
            score = 100 - min(100, variance / 10)
            return score
    
    @staticmethod
    def calculate_sharpness(image: np.ndarray) -> float:
        """
        Calculate image sharpness (Laplacian variance)
        
        Returns:
            sharpness: Higher = sharper
        """
        
        gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
        laplacian = cv2.Laplacian(gray, cv2.CV_64F)
        sharpness = laplacian.var()
        return float(sharpness)
    
    @staticmethod
    def calculate_edge_smoothness(image: np.ndarray) -> float:
        """
        Measure edge smoothness (less "pasted" look)
        
        Returns:
            smoothness: 0-1 (higher = smoother)
        """
        
        # Detect edges
        gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
        edges = cv2.Canny(gray, 50, 150)
        
        # Count harsh edges
        harsh_edges = np.sum(edges > 200)
        total_pixels = edges.size
        
        # Smoothness = fewer harsh edges
        smoothness = 1 - (harsh_edges / total_pixels)
        
        return float(smoothness)
    
    @staticmethod
    def calculate_all(image: np.ndarray) -> Dict:
        """Calculate all quality metrics"""
        
        return {
            "brisque_score": QualityMetrics.calculate_brisque(image),
            "sharpness": QualityMetrics.calculate_sharpness(image),
            "edge_smoothness": QualityMetrics.calculate_edge_smoothness(image)
        }


class DataPurgeProtocol:
    """
    Automatically purge temporary face data after processing
    """
    
    @staticmethod
    def purge_embeddings(temp_dir: str, identity_id: str):
        """
        Delete face embeddings and temporary data
        
        Args:
            temp_dir: Temporary storage directory
            identity_id: Unique ID for this processing session
        """
        
        print("🗑️  Purging temporary face data...")
        
        try:
            # Delete face embeddings
            embedding_path = os.path.join(temp_dir, f"{identity_id}_embedding.pkl")
            if os.path.exists(embedding_path):
                os.remove(embedding_path)
                print(f"  ✓ Deleted embedding: {embedding_path}")
            
            # Delete temporary images
            temp_image_path = os.path.join(temp_dir, f"{identity_id}_temp.jpg")
            if os.path.exists(temp_image_path):
                os.remove(temp_image_path)
                print(f"  ✓ Deleted temp image: {temp_image_path}")
            
            # Delete any cached face detections
            cache_path = os.path.join(temp_dir, f"{identity_id}_cache")
            if os.path.exists(cache_path):
                import shutil
                shutil.rmtree(cache_path)
                print(f"  ✓ Deleted cache: {cache_path}")
            
            print("✅ Data Purge Complete - Privacy Protected")
            
        except Exception as e:
            print(f"⚠️  Purge warning: {e}")
    
    @staticmethod
    def auto_purge_old_data(temp_dir: str, max_age_hours: int = 24):
        """
        Automatically purge data older than specified hours
        
        Args:
            temp_dir: Temporary storage directory
            max_age_hours: Maximum age in hours
        """
        
        import time
        
        current_time = time.time()
        max_age_seconds = max_age_hours * 3600
        
        if not os.path.exists(temp_dir):
            return
        
        purged_count = 0
        
        for filename in os.listdir(temp_dir):
            filepath = os.path.join(temp_dir, filename)
            
            if os.path.isfile(filepath):
                file_age = current_time - os.path.getmtime(filepath)
                
                if file_age > max_age_seconds:
                    try:
                        os.remove(filepath)
                        purged_count += 1
                    except Exception as e:
                        print(f"Could not delete {filepath}: {e}")
        
        if purged_count > 0:
            print(f"🗑️  Auto-purged {purged_count} old files from {temp_dir}")


# Example usage
if __name__ == "__main__":
    # Test identity validation
    validator = IdentityValidator()
    
    # Load test images
    reference = cv2.imread("reference.jpg")
    generated = cv2.imread("generated.jpg")
    
    # Validate
    is_valid, metrics = validator.validate(reference, generated)
    
    print("\nValidation Results:")
    print(f"  Valid: {is_valid}")
    print(f"  Similarity: {metrics['similarity_score']:.2%}")
    print(f"  Recommendation: {metrics['recommendation']}")
    
    # Quality metrics
    quality = QualityMetrics.calculate_all(generated)
    print("\nQuality Metrics:")
    print(f"  BRISQUE: {quality['brisque_score']:.2f}")
    print(f"  Sharpness: {quality['sharpness']:.2f}")
    print(f"  Smoothness: {quality['edge_smoothness']:.2%}")
