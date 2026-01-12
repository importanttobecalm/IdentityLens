package com.identitylens.app.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Identity Packet - Complete data structure for cloud submission
 */
data class IdentityPacket(
    @SerializedName("version")
    val version: String = "1.0",
    
    @SerializedName("captureId")
    val captureId: String = UUID.randomUUID().toString(),
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("image")
    val image: ImageData,
    
    @SerializedName("facialData")
    val facialData: FacialData,
    
    @SerializedName("segmentation")
    val segmentation: SegmentationData,
    
    @SerializedName("metadata")
    val metadata: CaptureMetadata,
    
    @SerializedName("qualityMetrics")
    val qualityMetrics: QualityMetrics
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
    
    companion object {
        fun fromJson(json: String): IdentityPacket {
            return Gson().fromJson(json, IdentityPacket::class.java)
        }
    }
}

data class ImageData(
    @SerializedName("cleanFace")
    val cleanFace: String,
    
    @SerializedName("resolution")
    val resolution: Resolution,
    
    @SerializedName("format")
    val format: String = "JPEG",
    
    @SerializedName("quality")
    val quality: Int = 95
)

data class Resolution(
    @SerializedName("width")
    val width: Int,
    
    @SerializedName("height")
    val height: Int
)

data class FacialData(
    @SerializedName("faceMesh")
    val faceMesh: FaceMesh,
    
    @SerializedName("boundingBox")
    val boundingBox: NormalizedBoundingBox,
    
    @SerializedName("eulerAngles")
    val eulerAngles: EulerAngles
)

data class FaceMesh(
    @SerializedName("landmarks")
    val landmarks: List<Landmark3D>,
    
    @SerializedName("totalPoints")
    val totalPoints: Int = 468
)

data class Landmark3D(
    @SerializedName("x")
    val x: Float,
    
    @SerializedName("y")
    val y: Float,
    
    @SerializedName("z")
    val z: Float,
    
    @SerializedName("confidence")
    val confidence: Float
)

data class NormalizedBoundingBox(
    @SerializedName("left")
    val left: Float,
    
    @SerializedName("top")
    val top: Float,
    
    @SerializedName("right")
    val right: Float,
    
    @SerializedName("bottom")
    val bottom: Float
)

data class EulerAngles(
    @SerializedName("pitch")
    val pitch: Float,
    
    @SerializedName("yaw")
    val yaw: Float,
    
    @SerializedName("roll")
    val roll: Float
)

data class SegmentationData(
    @SerializedName("backgroundMask")
    val backgroundMask: String,
    
    @SerializedName("resolution")
    val resolution: Resolution,
    
    @SerializedName("confidence")
    val confidence: Float
)

data class CaptureMetadata(
    @SerializedName("lighting")
    val lighting: LightingMetadata,
    
    @SerializedName("camera")
    val camera: CameraMetadata,
    
    @SerializedName("device")
    val device: DeviceMetadata
)

data class LightingMetadata(
    @SerializedName("luxValue")
    val luxValue: Double,
    
    @SerializedName("environment")
    val environment: String,
    
    @SerializedName("classification")
    val classification: String
)

data class CameraMetadata(
    @SerializedName("iso")
    val iso: Int,
    
    @SerializedName("exposureTime")
    val exposureTime: String,
    
    @SerializedName("focalLength")
    val focalLength: String,
    
    @SerializedName("aperture")
    val aperture: String? = null
)

data class DeviceMetadata(
    @SerializedName("model")
    val model: String,
    
    @SerializedName("osVersion")
    val osVersion: String,
    
    @SerializedName("orientation")
    val orientation: String
)

data class QualityMetrics(
    @SerializedName("overallScore")
    val overallScore: Double,
    
    @SerializedName("blurScore")
    val blurScore: Double,
    
    @SerializedName("angleScore")
    val angleScore: Double,
    
    @SerializedName("lightingScore")
    val lightingScore: Double,
    
    @SerializedName("faceSize")
    val faceSize: Double,
    
    @SerializedName("passed")
    val passed: Boolean
)
