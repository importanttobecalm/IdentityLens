"""
Test script for Cloud Inference Pipeline
"""

import json
import os
from flux_api_client import FluxPuLIDClient, GenerationMode

# Sample Identity Packet (from Step 1)
SAMPLE_IDENTITY_PACKET = {
    "version": "1.0",
    "captureId": "test-capture-123",
    "timestamp": "2026-01-12T18:00:00Z",
    "image": {
        "cleanFace": "base64_encoded_image_here...",  # Replace with actual base64
        "resolution": {"width": 1024, "height": 1024},
        "format": "jpeg",
        "quality": 95
    },
    "facialData": {
        "boundingBox": {
            "x": 256,
            "y": 200,
            "width": 512,
            "height": 512
        },
        "eulerAngles": {
            "pitch": 2.5,
            "yaw": -1.2,
            "roll": 0.8
        }
    },
    "qualityMetrics": {
        "overallScore": 0.92,
        "passed": True
    }
}

# Sample Master Prompt (from Step 2)
SAMPLE_MASTER_PROMPT = (
    "A person with the exact facial features from the reference image, "
    "standing on rainy 1920s Paris cobblestone street, wearing vintage "
    "three-piece suit, warm street lamp glow from left, wet pavement "
    "reflections, Art Deco buildings, evening blue hour, atmospheric rain, "
    "film noir aesthetic, photorealistic 8k, cinematic lighting, "
    "period-accurate details"
)

# Sample Negative Prompt
SAMPLE_NEGATIVE_PROMPT = (
    "deformed, bad anatomy, poorly drawn hands, modern clothing, "
    "contemporary architecture, smartphones, cars, dry surfaces, "
    "harsh shadows, cartoonish, anime, low quality, different person, "
    "wrong face"
)


def test_generation():
    """Test image generation"""
    
    # Get API key from environment
    api_key = os.getenv("FAL_API_KEY")
    if not api_key:
        print("❌ Error: FAL_API_KEY not set")
        print("   Set it with: export FAL_API_KEY=your_key_here")
        return
    
    # Create client
    print("🔧 Initializing Flux + PuLID client...")
    client = FluxPuLIDClient(api_key=api_key, provider="fal")
    
    # Test speed mode
    print("\n🚀 Testing SPEED mode (Flux schnell)...")
    print(f"   Prompt: {SAMPLE_MASTER_PROMPT[:80]}...")
    
    result = client.generate_with_retry(
        identity_packet=SAMPLE_IDENTITY_PACKET,
        master_prompt=SAMPLE_MASTER_PROMPT,
        negative_prompt=SAMPLE_NEGATIVE_PROMPT,
        mode=GenerationMode.SPEED,
        max_retries=3
    )
    
    if result.success:
        print(f"\n✅ Success!")
        print(f"   Image URL: {result.image_url}")
        print(f"   Inference Time: {result.inference_time:.2f}s")
        print(f"   Model: {result.model_version}")
        print(f"   Seed: {result.seed}")
    else:
        print(f"\n❌ Failed!")
        print(f"   Error Code: {result.error_code}")
        print(f"   Error Message: {result.error_message}")


def test_quality_mode():
    """Test quality mode generation"""
    
    api_key = os.getenv("FAL_API_KEY")
    if not api_key:
        return
    
    client = FluxPuLIDClient(api_key=api_key)
    
    print("\n🎨 Testing QUALITY mode (Flux dev)...")
    
    result = client.generate_image(
        identity_packet=SAMPLE_IDENTITY_PACKET,
        master_prompt=SAMPLE_MASTER_PROMPT,
        negative_prompt=SAMPLE_NEGATIVE_PROMPT,
        mode=GenerationMode.QUALITY
    )
    
    if result.success:
        print(f"✅ Quality mode: {result.inference_time:.2f}s")
    else:
        print(f"❌ Quality mode failed: {result.error_message}")


def save_test_packet():
    """Save test identity packet for API server testing"""
    
    with open("test_identity_packet.json", "w") as f:
        json.dump(SAMPLE_IDENTITY_PACKET, f, indent=2)
    
    print("💾 Saved test_identity_packet.json")


if __name__ == "__main__":
    print("=" * 80)
    print("IdentityLens Cloud Inference Pipeline - Test Suite")
    print("=" * 80)
    
    # Save test data
    save_test_packet()
    
    # Run tests
    test_generation()
    
    # Uncomment to test quality mode
    # test_quality_mode()
    
    print("\n" + "=" * 80)
