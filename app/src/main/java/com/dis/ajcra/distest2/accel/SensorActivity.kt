package com.dis.ajcra.distest2.accel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.dis.ajcra.distest2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class SensorActivity : AppCompatActivity() {
    companion object {
        var LOCATION_PERMISSION_CODE = 410
    }
    private lateinit var recognizeButton: Button
    private lateinit var ridenameField: EditText
    private lateinit var fingerprintButton: Button
    private lateinit var rideMatchText: TextView
    private lateinit var movementMatchText: TextView
    private var rideMatchID: UUID? = null
    private var movementMatchID: UUID? = null

    private var matchTimeFormat = SimpleDateFormat("MM/dd hh:mm:ss a")

    private lateinit var accelStore: AccelStore

    fun checkLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionDialog() {
        if (!checkLocationPermission(applicationContext)) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO),
                    LOCATION_PERMISSION_CODE)
        }
    }

    private var fingerprintOnClickListener = object: View.OnClickListener {
        override fun onClick(p0: View?) {
            startFingerprintCapture()
            fingerprintButton.text = "STOP"
            fingerprintButton.setOnClickListener {
                stopCapture()
                fingerprintButton.text = "FINGERPRINT"
                fingerprintButton.setOnClickListener(this)
            }
        }
    }

    private var recognizeOnClickListener = object: View.OnClickListener {
        override fun onClick(p0: View?) {
            startRecogntionCapture()
            recognizeButton.text = "STOP"
            recognizeButton.setOnClickListener {
                stopService(Intent(applicationContext, RideRecService::class.java))
                recognizeButton.text = "RECOGNIZE"
                recognizeButton.setOnClickListener(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        accelStore = AccelStore.GetInstance(applicationContext)

        ridenameField = findViewById(R.id.sensor_ridenameField)

        recognizeButton = findViewById(R.id.sensor_recognizeButton)
        recognizeButton.setOnClickListener(recognizeOnClickListener)

        fingerprintButton = findViewById(R.id.fingerprint_button)
        fingerprintButton.setOnClickListener(fingerprintOnClickListener)

        rideMatchText = findViewById(R.id.sensor_rideMatches)
        movementMatchText = findViewById(R.id.sensor_movementMatches)

        showPermissionDialog()
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch(Dispatchers.Main) {
            var rideMatches = accelStore.getRideMatches()
            var rideStr = ""
            rideMatches.await().forEach { rm ->
                rideStr += rm.name + ": " + matchTimeFormat.format(Date(rm.time)) + " : " + rm.distance + "\n"
            }
            rideMatchText.text = rideStr

            var movementMatches = accelStore.getMovementMatches()
            var movementStr = ""
            movementMatches.await().forEach { mm ->
                movementStr += mm.name + ": " + matchTimeFormat.format(Date(mm.time)) + ":" + mm.confidence + "\n"
            }
            movementMatchText.text = movementStr

            rideMatchID = accelStore.subscribeToRideMatches { rm ->
                rideMatchText.text = rm.name + ": " + matchTimeFormat.format(Date(rm.time)) + " : " + rm.distance + "\n" + rideMatchText.text.toString()
            }
            movementMatchID = accelStore.subscribeToMovementMatches { mm ->
                movementMatchText.text = mm.name + ": " + matchTimeFormat.format(Date(mm.time)) + ":" + mm.confidence + "\n" + movementMatchText.text.toString()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (rideMatchID != null) {
            accelStore.unsubscribeFromRideMatches(rideMatchID!!)
        }
        if (movementMatchID != null) {
            accelStore.unsubscribeFromMovementMatches(movementMatchID!!)
        }
    }

    fun startRecogntionCapture() {
        var intent = Intent(this, RideRecService::class.java)
        startService(intent)
    }

    fun startFingerprintCapture() {
        val serviceIntent = Intent(this, AccelService2::class.java)
        serviceIntent.putExtra("ridename", ridenameField.text.toString())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    fun stopCapture() {
        stopService(Intent(applicationContext, AccelService2::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
