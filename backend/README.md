# IdentityLens Backend - Cloud Inference Pipeline

Python backend server for Flux.1 + PuLID identity-preserving image generation.

## 🚀 Quick Start

### 1. Install Dependencies

```bash
cd backend
pip install -r requirements.txt
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env and add your FAL_API_KEY
```

### 3. Run Server

```bash
# Development
python api_server.py

# Production
gunicorn api_server:app --workers 4 --worker-class uvicorn.workers.UvicornWorker --bind 0.0.0.0:8000
```

### 4. Test

```bash
# Set API key
export FAL_API_KEY=your_fal_key_here

# Run tests
python test_inference.py
```

## 📡 API Endpoints

### Health Check

```http
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "provider": "fal.ai"
}
```

### Generate Image

```http
POST /api/generate
Content-Type: application/json
```

**Request:**
```json
{
  "identity_packet": { /* From Step 1 */ },
  "master_prompt": "A person with exact facial features...",
  "negative_prompt": "deformed, bad anatomy...",
  "mode": "speed",  // or "quality"
  "lighting_params": {
    "direction": "side",
    "ambient": "blue_night",
    "temperature": 3200
  },
  "enable_harmonization": true,
  "denoising_strength": 0.40
}
```

**Response (Success):**
```json
{
  "success": true,
  "image_url": "https://fal.media/files/...",
  "inference_time": 5.2,
  "model_version": "flux-schnell",
  "seed": 123456
}
```

**Response (Error):**
```json
{
  "success": false,
  "inference_time": 1.5,
  "error": {
    "code": 1001,
    "message": "Referans görüntüde yüz algılanamadı",
    "action": "Lütfen yüzünüzün net göründüğü bir fotoğraf çekin",
    "retry": false
  }
}
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `FAL_API_KEY` | Fal.ai API key | Required |
| `PORT` | Server port | 8000 |
| `API_PROVIDER` | API provider | fal |
| `DEFAULT_MODEL` | Default model | flux-schnell |
| `DENOISING_STRENGTH` | Harmonization strength | 0.40 |

### Model Modes

**Speed Mode (flux-schnell):**
- Inference: 4-6 seconds
- Steps: 4
- Guidance: 3.5
- Cost: ~$0.025/image

**Quality Mode (flux-dev):**
- Inference: 8-10 seconds
- Steps: 30
- Guidance: 7.5
- Cost: ~$0.055/image

## 🏗️ Architecture

```
Android App
    ↓
FastAPI Server (api_server.py)
    ↓
FluxPuLIDClient (flux_api_client.py)
    ↓
Fal.ai API → Flux.1 + PuLID
    ↓
Harmonization (img2img)
    ↓
Final Image
```

## 🎨 PuLID Configuration

### Identity Fidelity

```python
PULID_CONFIG = {
    "fidelity_weight": 0.85,  # 0.7-0.95 range
    "face_detection": "retinaface",
    "embedding": "arcface_r100"
}
```

**Weight Guidelines:**
- **0.70-0.80**: More artistic freedom, less resemblance
- **0.85**: **Recommended** - Balanced
- **0.90-0.95**: Maximum resemblance, may cause artifacts

### Harmonization (Uncanny Valley Prevention)

```python
HARMONIZATION = {
    "denoising_strength": 0.40,  # 0.35-0.45 range
    "preserve_skin_texture": True,
    "preserve_pores": True
}
```

**Critical:** Keep denoising in 0.35-0.45 range to avoid over-smoothing.

## ❌ Error Codes

| Code | Description | Retry? |
|------|-------------|--------|
| 1001 | No face detected | No |
| 1002 | Low quality image | Yes |
| 2001 | Inference timeout | Yes |
| 2002 | API rate limit | Yes |
| 3001 | NSFW content | No |
| 4001 | API error | Yes |

## 📊 Performance Optimization

### Target Timeline

```
1. Upload: 0.5s
2. Face embedding: 0.5s
3. Flux inference: 4-5s (schnell)
4. Harmonization: 2s
5. Download: 0.5s
------------------------
Total: 7.5-8.5s ✅
```

### Tips

1. **Use Speed Mode**: Default for mobile users
2. **Enable Caching**: Cache face embeddings (1 hour)
3. **Async Processing**: For batch operations
4. **CDN**: Serve generated images via CDN

## 🚢 Deployment

### Docker

```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

CMD ["gunicorn", "api_server:app", "--workers", "4", "--worker-class", "uvicorn.workers.UvicornWorker", "--bind", "0.0.0.0:8000"]
```

### Railway/Render

1. Connect GitHub repo
2. Set environment variables:
   - `FAL_API_KEY`
   - `PORT=8000`
3. Deploy automatically

### Cloud Run (GCP)

```bash
gcloud run deploy identitylens-api \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

## 🧪 Testing

### Unit Tests

```bash
pytest tests/ -v --cov=.
```

### Load Testing

```bash
# Install locust
pip install locust

# Run load test
locust -f tests/load_test.py
```

## 📝 API Providers

### Fal.ai (Recommended)

✅ Fastest inference (4-6s)
✅ Built-in PuLID support
✅ Automatic scaling
✅ Fair pricing

### Replicate

✅ Good documentation
✅ Reliable infrastructure
⚠️ Slower (8-12s)
⚠️ Manual PuLID setup

### ComfyUI (Self-Hosted)

✅ Full control
✅ No per-request costs
⚠️ Requires GPU server
⚠️ Manual maintenance

## 🔐 Security

### Production Checklist

- [ ] Enable API key authentication
- [ ] Configure CORS properly
- [ ] Add rate limiting
- [ ] Enable HTTPS
- [ ] Sanitize user inputs
- [ ] Monitor API usage
- [ ] Set up error logging

### Example: API Key Auth

```python
from fastapi import Security, HTTPException
from fastapi.security.api_key import APIKeyHeader

api_key_header = APIKeyHeader(name="X-API-Key")

@app.post("/api/generate")
async def generate(request: GenerationRequest, api_key: str = Security(api_key_header)):
    if api_key != os.getenv("SERVER_API_KEY"):
        raise HTTPException(status_code=403, detail="Invalid API key")
    # ...
```

## 📊 Monitoring

### Metrics to Track

- Request count
- Success rate
- Average inference time
- Error rate by code
- API costs

### Example: Prometheus

```python
from prometheus_client import Counter, Histogram

generation_requests = Counter('generation_requests_total', 'Total generation requests')
inference_time = Histogram('inference_time_seconds', 'Inference time')

@app.post("/api/generate")
async def generate(...):
    generation_requests.inc()
    with inference_time.time():
        result = flux_client.generate(...)
    # ...
```

## 🐛 Troubleshooting

### "FAL_API_KEY not set"

```bash
export FAL_API_KEY=your_key_here
```

### Slow inference (>15s)

- Switch to `flux-schnell` model
- Check network latency
- Reduce image size
- Disable harmonization temporarily

### "No face detected" errors

- Check input image quality
- Verify face is visible
- Check image format (JPEG/PNG)
- Test with sample images

## 📚 Resources

- [Fal.ai Documentation](https://fal.ai/docs)
- [Flux.1 Model Card](https://github.com/black-forest-labs/flux)
- [PuLID Paper](https://arxiv.org/abs/...)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

---

**IdentityLens Cloud Inference Pipeline** - Powered by Flux.1 + PuLID 🚀
