package com.dis.ajcra.distest2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.opengl.Matrix
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.experimental.async
import tutorial.Acceleration
import java.io.File

class SensorActivity : AppCompatActivity() {
    companion object {
        const val ACCEL_DELAY = 16L
        const val ACCEL_MULT = 32726.0f/10.0f
    }
    private lateinit var coordText: TextView
    private lateinit var toggleButton: Button

    private var r = FloatArray(16, {0.0f})
    private var rinv = FloatArray(16, {0.0f})
    private var trueAcceleration = FloatArray(4, {0.0f})

    private var gravity: FloatArray? = null
    private var geoMagnetic: FloatArray? = null
    private var linearAcceleration = FloatArray(4, {0.0f})
    private var netAcceleration = FloatArray(3, {0.0f})
    private var netAccelCount: Int = 0
    private lateinit var accelDataBuilder: Acceleration.AccelerationData.Builder

    //temp
    private lateinit var cognitoManager: CognitoManager
    private lateinit var cfm: CloudFileManager

    private var sensorListener = object: SensorEventListener2 {
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

        override fun onFlushCompleted(p0: Sensor?) {

        }

        override fun onSensorChanged(evt: SensorEvent?) {
            Log.d("STATE", "SENSOR CALLED")
            if (evt != null) {
                when (evt.sensor.type) {
                    Sensor.TYPE_GRAVITY -> {
                        gravity = evt.values
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geoMagnetic = evt.values
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        linearAcceleration[0] = evt.values[0]
                        linearAcceleration[1] = evt.values[1]
                        linearAcceleration[2] = evt.values[2]
                        linearAcceleration[3] = 0f
                    }
                }
                try {
                    if (gravity != null && geoMagnetic != null && linearAcceleration != null) {
                        SensorManager.getRotationMatrix(r, null, gravity, geoMagnetic);
                        Matrix.invertM(rinv, 0, r, 0)
                        Matrix.multiplyMV(trueAcceleration, 0, rinv, 0, linearAcceleration, 0)
                        //coordText.text = "X: " + trueAcceleration[0] + "\nY: " + trueAcceleration[1] + "\nZ: " + trueAcceleration[2] + "\nTheta: " + trueAcceleration[3]
                        synchronized(netAcceleration) {
                            netAccelCount++
                            var i = 0
                            while (i < 3) {
                                netAcceleration[i] = netAcceleration[i] * (netAccelCount - 1) / (netAccelCount) + trueAcceleration[i] / netAccelCount
                                i++
                            }
                        }
                    } else {

                    }
                } catch(ex: Exception) {
                    Log.d("STATE", "EX: " + ex.message)
                }
            }
        }

    }

    private lateinit var sensorManager: SensorManager

    private lateinit var gravitySensor: Sensor
    private lateinit var geoMagneticSensor: Sensor
    private lateinit var linearAccelerationSensor: Sensor
    private var accelDataTimerHandler: Handler = Handler()

    fun accelToInt(accel: Float): Int {
        return (accel * ACCEL_MULT).toInt()
    }

    private var accelDataRunner: Runnable = object: Runnable {
        override fun run() {
            accelDataTimerHandler.postDelayed(this, ACCEL_DELAY)

            synchronized(netAcceleration) {
                netAccelCount = 0
                accelDataBuilder.addX(accelToInt(netAcceleration[0]))
                accelDataBuilder.addY(accelToInt(netAcceleration[1]))
                accelDataBuilder.addZ(accelToInt(netAcceleration[2]))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        cognitoManager = CognitoManager.GetInstance(applicationContext)
        cfm = CloudFileManager(cognitoManager, applicationContext)

        accelDataBuilder = Acceleration.AccelerationData.newBuilder()

        coordText = findViewById(R.id.sensor_coords)
        toggleButton = findViewById(R.id.sensor_toggle)
        toggleButton.setOnClickListener {
            if (toggleButton.text == "ACTIVATE") {
                toggleButton.text = "DEACTIVATE"
                startCollection()
            } else {
                toggleButton.text = "ACTIVATE"
                stopCollection()
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        geoMagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    }

    fun startCollection() {
        sensorManager.registerListener(sensorListener, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(sensorListener, geoMagneticSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(sensorListener, linearAccelerationSensor, SensorManager.SENSOR_DELAY_FASTEST)

        accelDataTimerHandler.postDelayed(accelDataRunner, ACCEL_DELAY)
    }


    fun stopCollection() {
        sensorManager.unregisterListener(sensorListener)
        var accelDataMsg = accelDataBuilder.build()
        var i = 0
        while (i < accelDataMsg.xCount) {
            Log.d("STATE", "X: " + accelDataMsg.getX(i) + "  Y: " + accelDataMsg.getY(i) + "  Z: " + accelDataMsg.getZ(i))
            i++
        }
        var dataStr = accelDataMsg.toByteString()
        var file = File(applicationContext.filesDir, "accels")
        file.writeBytes(dataStr.toByteArray())
        async {
            cfm.upload("accels", file.toURI(), object : CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {

                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

                }

                override fun onStateChanged(id: Int, state: TransferState?) {

                }

                override fun onComplete(id: Int, file: File) {
                    Log.d("STATE", "Upload complete")
                }
            })
        }
        //Log.d("STATE", "DATA: " + dataStr)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
