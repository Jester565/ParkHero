package com.dis.ajcra.distest2

import android.content.pm.PackageManager
import android.graphics.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.ContextMenu
import android.view.Menu
import android.view.View
import android.widget.Toast
import com.otaliastudios.cameraview.*
import java.util.jar.Manifest

class CameraActivity : AppCompatActivity() {
    companion object {
        val REQUEST_CAMERA_PERMISSION = 1
    }
    class CameraViewCB: CameraListener {
        constructor() {

        }

        override fun onCameraOpened(options: CameraOptions?) {
            super.onCameraOpened(options)
        }
    }

    private lateinit var cameraView: CameraView
    private lateinit var toolBar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        cameraView = findViewById(R.id.camera)
        cameraView.addCameraListener(CameraViewCB())
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM)
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER)
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraView.start()
        }
        else {
            ActivityCompat.requestPermissions(this, Array<String>(1, { i -> android.Manifest.permission.CAMERA }), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (permissions.size == 0 || grantResults.size == 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }
}
