package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(context: Context) : SensorEventListener {

    private var mListener: OnShakeListener? = null // Listener for shake events
    private var mShakeTimestamp: Long = 0 // Timestamp of last shake event
    private var mShakeCount = 0 // Count of shake events

    interface OnShakeListener {
        fun onShake(count: Int) // Callback method when shake event occurs
    }
    // Method set the listener for shake events
    fun setOnShakeListener(listener: OnShakeListener) {
        this.mListener = listener
    }
    // Method called when the sensor changes (not used)
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }
    // Method called  when sensor data changes (when the device is moved)
    override fun onSensorChanged(event: SensorEvent) {
        if (mListener != null) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            // gForce will be close to 1 when there is no movement.
            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()

                // Ignore shake events too close to each other (500ms)
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }

                // Reset the shake count after 3 seconds of no shakes
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0
                }

                mShakeTimestamp = now // Update the timestamp of last shake event
                // Increment the shake count
                // and keeps track how many number of shakes events are detected
                mShakeCount++

                // Notify the listener about the shake event
                mListener?.onShake(mShakeCount)
            }
        }
    }

    // Containing constants
    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SHAKE_COUNT_RESET_TIME_MS = 3000
    }
    // This block of code enables the accelerometer sensor listener
    // When shake detector is enabled
    init {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
}