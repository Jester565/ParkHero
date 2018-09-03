package com.dis.ajcra.distest2

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaRecorder
import android.opengl.Matrix
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.experimental.async
import tutorial.Acceleration
import java.io.File
import java.util.*
import kotlin.concurrent.fixedRateTimer


class AccelService2 : Service() {
    companion object {
        const val ACCEL_DELAY = 33L
        const val ACCEL_MULT = 32726.0f/10.0f
        const val ACCEL_CHANNEL_ID = "DisAccel"
    }

    //Data structures for calculations the netAcceleration
    private var r = FloatArray(16, {0.0f})
    private var rinv = FloatArray(16, {0.0f})
    private var trueAcceleration = FloatArray(4, {0.0f})
    private var gravity: FloatArray? = null
    private var geoMagnetic: FloatArray? = null
    private var linearAcceleration = FloatArray(4, {0.0f})
    private var netAcceleration = FloatArray(3, {0.0f})
    private var netAccelCount: Int = 0
    private var startTime: Date? = null
    private var avgLongitude: Double = 0.0
    private var avgLatitude: Double = 0.0
    private var gpsRecordCount: Int = 0

    private lateinit var sensorManager: SensorManager
    private lateinit var gravitySensor: Sensor
    private lateinit var geoMagneticSensor: Sensor
    private lateinit var linearAccelerationSensor: Sensor
    private lateinit var locationManager: LocationManager

    //Protobuf message builder for sending acceleration data
    private lateinit var accelDataBuilder: Acceleration.AccelerationData.Builder

    private lateinit var cognitoManager: CognitoManager
    private lateinit var cfm: CloudFileManager

    private var running = false

    private var ridename: String? = null

    private var audioRecorder: MediaRecorder? = null
    private var audioPath: String? = null

    private fun accelToInt(accel: Float): Int {
        return (accel * ACCEL_MULT).toInt()
    }

    private var sensorListener = object: SensorEventListener2 {
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) { }

        override fun onFlushCompleted(p0: Sensor?) { }

        override fun onSensorChanged(evt: SensorEvent?) {
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
                        synchronized(netAcceleration) {
                            netAccelCount++
                            var i = 0
                            while (i < 3) {
                                netAcceleration[i] = netAcceleration[i] * (netAccelCount - 1) / (netAccelCount) + trueAcceleration[i] / netAccelCount
                                i++
                            }
                        }
                    }
                } catch(ex: Exception) {
                    Log.e("STATE", "EX: " + ex.message)
                }
            }
        }
    }

    private var locationListener = object: LocationListener {
        override fun onLocationChanged(location: Location?) {
            if (location != null) {
                gpsRecordCount++
                avgLongitude = avgLongitude * (gpsRecordCount - 1).toDouble()/gpsRecordCount + location.longitude * 1.0/gpsRecordCount
                avgLatitude = avgLatitude * (gpsRecordCount - 1).toDouble()/gpsRecordCount + location.latitude * 1.0/gpsRecordCount
            }
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}

        override fun onProviderEnabled(p0: String?) {}

        override fun onProviderDisabled(p0: String?) {}
    }

    fun startCollection() {
        Log.d("ACCEL", "Starting collection")
        sensorManager.registerListener(sensorListener, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(sensorListener, geoMagneticSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(sensorListener, linearAccelerationSensor, SensorManager.SENSOR_DELAY_FASTEST)

        running = true
        startTime = Date()

        gpsRecordCount = 0

        if (ridename != null) {
            audioRecorder = MediaRecorder()
            audioPath = this.filesDir.toString() + "/" + ridename + ".3gpp"
            Log.d("ACCEL", "AudioPath: " + audioPath)

            audioRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            audioRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            audioRecorder!!.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
            audioRecorder!!.setOutputFile(audioPath!!)

            audioRecorder!!.prepare()
            audioRecorder!!.start()

            fixedRateTimer(period=ACCEL_DELAY, action={
                if (!running) {
                    cancel()
                }
                synchronized(netAcceleration) {
                    netAccelCount = 0
                    accelDataBuilder.addMillis(Date().time)
                    accelDataBuilder.addX(accelToInt(netAcceleration[0]))
                    accelDataBuilder.addY(accelToInt(netAcceleration[1]))
                    accelDataBuilder.addZ(accelToInt(netAcceleration[2]))
                }
            })
        }
    }


    fun stopCollection() {
        Log.d("ACCEL", "Stopping collection")
        running = false
        sensorManager.unregisterListener(sensorListener)
        locationManager.removeUpdates(locationListener)
        audioRecorder?.stop()
        audioRecorder?.reset()
        audioRecorder?.release()
        audioRecorder = null

        if (audioPath != null) {
            var audioFile = File(audioPath)
            audioPath = null
            async {
                cfm.upload("recs/" + ridename + ".3gpp", audioFile.toURI(), object : CloudFileListener() {
                    override fun onComplete(id: Int, file: File) {
                        super.onComplete(id, file)
                        Log.d("ACCEL", "Audio upload complete")
                    }
                })
            }
        }


        if (ridename == null) {
            async {
                var uuid = UUID.randomUUID().toString()
                uploadAcceleration("accels/" + cognitoManager.federatedID + "/" + uuid, uuid)
                accelDataBuilder.clear()
            }
        } else {
            async {
                var uuid = UUID.randomUUID().toString()
                uploadAcceleration("rideAccels/" + ridename, ridename!!)
                accelDataBuilder.clear()
                ridename = null
            }
        }
    }

    fun uploadAcceleration(objKey: String, fileName: String) {
        accelDataBuilder.setLongitude(avgLongitude)
        accelDataBuilder.setLatitude(avgLatitude)
        var accelDataMsg = accelDataBuilder.build()
        var dataStr = accelDataMsg.toByteString()
        var file = File(applicationContext.filesDir, fileName)
        file.writeBytes(dataStr.toByteArray())
        async {
            cfm.upload(objKey, file.toURI(), object : CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {
                    Log.d("ACCEL", "Upload error: " + ex?.message)
                }

                override fun onComplete(id: Int, file: File) {
                    Log.d("ACCEL", "Upload complete: " + objKey)
                }
            })
        }
    }

    fun checkLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        ridename = intent?.extras?.getString("ridename")
        createNotificationChannel(AccelService2.ACCEL_CHANNEL_ID, "Acceleration", "Acceleration")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notification = Notification.Builder(applicationContext, AccelService2.ACCEL_CHANNEL_ID)
                    .setContentTitle("ACCEL")
                    .setContentText("Monitoring Acceleration")
                    .build()
            startForeground(101, notification)
        } else {
            val notification = NotificationCompat.Builder(this)
                    .setContentTitle("ACCEL")
                    .setContentText("Monitoring Acceleration")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

            startForeground(101, notification)
        }

        cognitoManager = CognitoManager.GetInstance(applicationContext)
        cfm = CloudFileManager(cognitoManager, applicationContext)

        accelDataBuilder = Acceleration.AccelerationData.newBuilder()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        geoMagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (checkLocationPermission(applicationContext)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10f, locationListener)
        }
        startCollection()


        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCollection()
    }

    private fun createNotificationChannel(id: String, name: String, desc: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(id, name, importance)
            mChannel.description = desc
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }
}
