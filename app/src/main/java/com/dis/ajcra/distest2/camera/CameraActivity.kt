package com.dis.ajcra.distest2.camera

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.R

class CameraActivity : FragmentActivity() {
    private lateinit var cameraFragment: CameraFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        var videoEnabled = true
        var galleryEnabled = true
        if (intent.extras != null) {
            if (intent.extras.containsKey("videoEnabled"))
                videoEnabled = intent.extras.getBoolean("videoEnabled")
            if (intent.extras.containsKey("galleryEnabled"))
                galleryEnabled = intent.extras.getBoolean("galleryEnabled")
        }
        cameraFragment = CameraFragment.GetInstance(videoEnabled, galleryEnabled)
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.cameraactivity_layout, cameraFragment).commit()
    }
}
