package com.identitylens.app.metadata

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Light Sensor Manager for lighting environment detection
 */
class LightSensorManager(context: Context) : SensorEventListener {
    
    private val sensorManager: SensorManager = 
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val lightSensor: Sensor? = 
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    
    private var currentLuxValue: Double = 0.0
    private var onLuxChanged: ((Double) -> Unit)? = null
    
    /**
     * Start listening to light sensor
     */
    fun start(onLuxChanged: (Double) -> Unit) {
        this.onLuxChanged = onLuxChanged
        lightSensor?.let {
            sensorManager.registerListener(
                this, 
                it, 
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }
    
    /**
     * Stop listening
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }
    
    /**
     * Get current lux value
     */
    fun getCurrentLux(): Double = currentLuxValue
    
    /**
     * Classify lighting environment
     */
    fun classifyEnvironment(luxValue: Double): String {
        return when {
            luxValue < 10 -> "Very Dark"
            luxValue < 50 -> "Dark"
            luxValue < 200 -> "Dim"
            luxValue < 1000 -> "Optimal"
            luxValue < 10000 -> "Bright"
            else -> "Very Bright"
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            currentLuxValue = event.values[0].toDouble()
            onLuxChanged?.invoke(currentLuxValue)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }
    
    /**
     * Check if light sensor is available
     */
    fun isAvailable(): Boolean = lightSensor != null
}
