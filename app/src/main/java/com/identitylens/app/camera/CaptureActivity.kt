package com.identitylens.app.camera

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.identitylens.app.R
import com.identitylens.app.metadata.LightSensorManager
import com.identitylens.app.models.*
import com.identitylens.app.quality.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Capture Activity - Smart image capture with real-time quality feedback
 */
class CaptureActivity : AppCompatActivity() {
    
    private lateinit var previewView: PreviewView
    private lateinit var textViewFeedback: TextView
    private lateinit var buttonCapture: Button
    
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var lightSensorManager: LightSensorManager
    
    private var imageCapture: ImageCapture? = null
    private var currentLuxValue: Double = 400.0
    
    // ML Kit & Quality Analyzers
    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }
    
    private val qualityAnalyzer = ImageQualityAnalyzer()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        
        previewView = findViewById(R.id.previewView)
        textViewFeedback = findViewById(R.id.textViewFeedback)
        buttonCapture = findViewById(R.id.buttonCapture)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        lightSensorManager = LightSensorManager(this)
        
        setupCamera()
        setupButtons()
        startLightSensor()
    }
    
    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            // Image analysis for real-time feedback
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer())
                }
            
            // Image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun setupButtons() {
        buttonCapture.setOnClickListener {
            captureImage()
        }
    }
    
    private fun startLightSensor() {
        lightSensorManager.start { lux ->
            currentLuxValue = lux
            runOnUiThread {
                val environment = lightSensorManager.classifyEnvironment(lux)
                updateLightingFeedback(environment)
            }
        }
    }
    
    private fun updateLightingFeedback(environment: String) {
        // Update UI with lighting environment
    }
    
    /**
     * Real-time face analyzer for quality feedback
     */
    private inner class FaceAnalyzer : ImageAnalysis.Analyzer {
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                
                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            val face = faces[0]
                            runOnUiThread {
                                updateFeedback(face, imageProxy)
                            }
                        } else {
                            runOnUiThread {
                                textViewFeedback.text = "No face detected"
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
        
        private fun updateFeedback(face: com.google.mlkit.vision.face.Face, imageProxy: ImageProxy) {
            // Quick quality check for feedback
            val angleValidator = com.identitylens.app.quality.FaceAngleValidator()
            val angleResult = angleValidator.validate(face)
            
            val feedback = if (angleResult.isValid) {
                "✓ Face aligned"
            } else {
                angleValidator.getFeedback(angleResult)
            }
            
            textViewFeedback.text = feedback
        }
    }
    
    /**
     * Capture image and process
     */
    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                @androidx.camera.core.ExperimentalGetImage
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    lifecycleScope.launch {
                        try {
                            processAndExportImage(imageProxy)
                        } catch (e: Exception) {
                            Log.e(TAG, "Image processing failed", e)
                            runOnUiThread {
                                Toast.makeText(
                                    this@CaptureActivity,
                                    "Processing failed: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } finally {
                            imageProxy.close()
                        }
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed", exception)
                }
            }
        )
    }
    
    /**
     * Process captured image and create Identity Packet
     */
    @androidx.camera.core.ExperimentalGetImage
    private suspend fun processAndExportImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        // Detect face
        val faces = faceDetector.process(image).await()
        if (faces.isEmpty()) {
            throw Exception("No face detected in captured image")
        }
        
        val face = faces[0]
        
        // Perform quality analysis
        val qualityResult = qualityAnalyzer.analyze(
            imageProxy,
            face,
            currentLuxValue
        )
        
        if (!qualityResult.passed) {
            val feedback = qualityResult.getFeedback().joinToString("\n")
            throw Exception("Quality check failed:\n$feedback")
        }
        
        // Create Identity Packet (simplified - full implementation would include face mesh)
        val identityPacket = createIdentityPacket(imageProxy, face, qualityResult)
        
        // Export JSON
        val json = identityPacket.toJson()
        Log.d(TAG, "Identity Packet created: ${json.take(200)}...")
        
        runOnUiThread {
            Toast.makeText(
                this@CaptureActivity,
                "✓ Image captured successfully!",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // TODO: Upload to cloud API
        // uploadToCloud(json)
    }
    
    private fun createIdentityPacket(
        imageProxy: ImageProxy,
        face: com.google.mlkit.vision.face.Face,
        qualityResult: QualityAnalysisResult
    ): IdentityPacket {
        // Simplified - in production, add face mesh extraction
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        
        return IdentityPacket(
            timestamp = timestamp,
            image = ImageData(
                cleanFace = "base64_placeholder",
                resolution = Resolution(imageProxy.width, imageProxy.height)
            ),
            facialData = FacialData(
                faceMesh = FaceMesh(landmarks = emptyList()),
                boundingBox = NormalizedBoundingBox(
                    left = face.boundingBox.left.toFloat() / imageProxy.width,
                    top = face.boundingBox.top.toFloat() / imageProxy.height,
                    right = face.boundingBox.right.toFloat() / imageProxy.width,
                    bottom = face.boundingBox.bottom.toFloat() / imageProxy.height
                ),
                eulerAngles = EulerAngles(
                    pitch = face.headEulerAngleX,
                    yaw = face.headEulerAngleY,
                    roll = face.headEulerAngleZ
                )
            ),
            segmentation = SegmentationData(
                backgroundMask = "base64_placeholder",
                resolution = Resolution(512, 512),
                confidence = 0.9f
            ),
            metadata = CaptureMetadata(
                lighting = LightingMetadata(
                    luxValue = currentLuxValue,
                    environment = lightSensorManager.classifyEnvironment(currentLuxValue),
                    classification = "Captured"
                ),
                camera = CameraMetadata(
                    iso = 200,
                    exposureTime = "1/60",
                    focalLength = "4.2mm"
                ),
                device = DeviceMetadata(
                    model = "${Build.MANUFACTURER} ${Build.MODEL}",
                    osVersion = "Android ${Build.VERSION.RELEASE}",
                    orientation = "PORTRAIT"
                )
            ),
            qualityMetrics = QualityMetrics(
                overallScore = qualityResult.overallScore,
                blurScore = qualityResult.blurResult.qualityScore,
                angleScore = qualityResult.angleResult.overallScore,
                lightingScore = qualityResult.lightingResult.score,
                faceSize = qualityResult.faceSizeResult.faceRatio,
                passed = qualityResult.passed
            )
        )
    }
    
    override fun onResume() {
        super.onResume()
        lightSensorManager.start { lux ->
            currentLuxValue = lux
        }
    }
    
    override fun onPause() {
        super.onPause()
        lightSensorManager.stop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceDetector.close()
    }
    
    companion object {
        private const val TAG = "CaptureActivity"
    }
}

// Extension function for Task await
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.tasks.await(this)
}
