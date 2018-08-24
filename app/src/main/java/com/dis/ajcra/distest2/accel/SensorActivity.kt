package com.dis.ajcra.distest2.accel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.dis.ajcra.distest2.AccelService2
import com.dis.ajcra.distest2.R




class SensorActivity : AppCompatActivity() {
    companion object {
        var LOCATION_PERMISSION_CODE = 410
    }
    private lateinit var recognizeButton: Button
    private lateinit var ridenameField: EditText
    private lateinit var fingerprintButton: Button

    fun checkLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionDialog() {
        if (!checkLocationPermission(applicationContext)) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_CODE)
        }
    }

    private var fingerprintOnClickListener = object: View.OnClickListener {
        override fun onClick(p0: View?) {
            startFingerprintCapture()
            recognizeButton.isEnabled = false
            fingerprintButton.text = "STOP"
            fingerprintButton.setOnClickListener {
                stopCapture()
                recognizeButton.isEnabled = true
                fingerprintButton.text = "FINGERPRINT"
                fingerprintButton.setOnClickListener(this)
            }
        }
    }

    private var recognizeOnClickListener = object: View.OnClickListener {
        override fun onClick(p0: View?) {
            startRecogntionCapture()
            fingerprintButton.isEnabled = false
            recognizeButton.text = "STOP"
            recognizeButton.setOnClickListener {
                stopCapture()
                fingerprintButton.isEnabled = true
                recognizeButton.text = "RECOGNIZE"
                recognizeButton.setOnClickListener(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        ridenameField = findViewById(R.id.sensor_ridenameField)

        recognizeButton = findViewById(R.id.sensor_recognizeButton)
        recognizeButton.setOnClickListener(recognizeOnClickListener)

        fingerprintButton = findViewById(R.id.fingerprint_button)
        fingerprintButton.setOnClickListener(fingerprintOnClickListener)

        showPermissionDialog()
    }

    fun startRecogntionCapture() {
        var intent = Intent(applicationContext, AccelService2::class.java)
        startService(intent)
    }

    fun startFingerprintCapture() {
        var intent = Intent(applicationContext, AccelService2::class.java)
        intent.putExtra("ridename", ridenameField.text.toString())
        startService(intent)
    }

    fun stopCapture() {
        stopService(Intent(applicationContext, AccelService2::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
