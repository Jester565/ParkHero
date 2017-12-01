package com.dis.ajcra.distest2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Camera
import android.icu.lang.UCharacter.GraphemeClusterBreak.V
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.transition.Visibility
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.View
import android.view.animation.Animation
import android.widget.*
import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer
import com.amazonaws.mobileconnectors.s3.transferutility.*
import com.amazonaws.services.s3.AmazonS3Client
import com.dis.ajcra.distest2.CameraActivity.Companion.REQUEST_CAMERA_PERMISSION
import com.google.api.client.util.DateTime
import com.otaliastudios.cameraview.*
import id.zelory.compressor.Compressor
import kotlinx.coroutines.experimental.async
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.jar.Manifest
import kotlin.concurrent.scheduleAtFixedRate

class CameraActivity : AppCompatActivity() {
    companion object {
        val REQUEST_CAMERA_PERMISSION = 1
    }

    class DlCameraListener: CameraListener {
        private var cognitoManager: CognitoManager
        private var transferUtil: TransferUtility
        private var appCtx: Context
        private var compressor: Compressor
        private var transferListener: TransferListener?
        constructor(cognitoManager: CognitoManager, appContext: Context, transferListener: TransferListener? = null) {
            this.cognitoManager = cognitoManager
            this.appCtx = appContext
            this.transferListener = transferListener
            var s3Client = AmazonS3Client(this.cognitoManager.credentialsProvider)
            transferUtil = TransferUtility(s3Client, this.appCtx)
            compressor = Compressor(appContext)
            compressor.setCompressFormat(Bitmap.CompressFormat.WEBP)
            compressor.setQuality(70)
        }

        override fun onCameraOpened(options: CameraOptions?) {
            super.onCameraOpened(options)
        }

        override fun onPictureTaken(jpeg: ByteArray?) {
            async {
                var time = System.currentTimeMillis()
                //Log.d("STATE", "Picture taken")

                var jpegFile = File(appCtx.filesDir,  UUID.randomUUID().toString() + ".webp")
                if (jpeg != null) {
                    jpegFile.writeBytes(jpeg)
                }

                var compFile = compressor.compressToFile(jpegFile)

                var objKey = cognitoManager.federatedID + "/" + compFile.name
                //Log.d("STATE", "Time: " + (System.currentTimeMillis() - time))
                Log.d("STATE", "File Name: " + compFile.name)
                Log.d("STATE", "File Len: " + compFile.length())
                Log.d("STATE", "ObjKey: " + objKey)
                var transferObserver = transferUtil.upload("disneyapp", objKey, compFile)
                if (transferListener != null) {
                    transferObserver.setTransferListener(transferListener)
                }
                /*
                var observers = transferUtil.getTransfersWithType(TransferType.ANY)
                for (observer in observers) {
                    Log.d("STATE", "Observer: " + observer.absoluteFilePath + " -- " + observer.state + " -- " + observer.bytesTransferred)
                }
                */

            }
        }

        override fun onVideoTaken(video: File?) {
            Log.d("STATE", "Video captured")
        }
    }

    private lateinit var cameraView: CameraView
    private lateinit var pictureButton: FloatingActionButton
    private lateinit var recordButton: FloatingActionButton
    private lateinit var galleryButton: FloatingActionButton
    private lateinit var switchCameraButton: ImageButton
    private lateinit var flashButton: ImageButton
    //private lateinit var brightnessSlider: SeekBar
    private lateinit var pictureBar: RelativeLayout
    private lateinit var recordBar: RelativeLayout
    private lateinit var durationText: TextView
    private lateinit var stopButton: FloatingActionButton
    private lateinit var imgLayout: LinearLayout
    private var recordTimer: Timer = Timer()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        var cognitoManager = CognitoManager.GetInstance(applicationContext)

        pictureBar = findViewById(R.id.camera_lowbar)
        recordBar = findViewById(R.id.camera_recordlowbar)
        durationText = findViewById(R.id.camera_durationtext)
        stopButton = findViewById(R.id.camera_stopbutton)
        imgLayout = findViewById(R.id.camera_imglayout)

        cameraView = findViewById(R.id.camera_camera)
        cameraView.addCameraListener(object: CameraListener() {
            override fun onPictureTaken(jpeg: ByteArray?) {
                if (jpeg != null) {
                    var imgView = ImageView(this@CameraActivity)
                    var bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                    imgView.setImageBitmap(bmp)
                    imgLayout.addView(imgView)
                }
            }
        })
        cameraView.addCameraListener(DlCameraListener(cognitoManager, applicationContext, object: TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

            }

            override fun onStateChanged(id: Int, state: TransferState?) {

            }

            override fun onError(id: Int, ex: Exception?) {

            }
        }))
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM)
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER)

        pictureButton = findViewById(R.id.camera_picturebutton)
        pictureButton.setOnClickListener {
            cameraView.capturePicture()
        }

        recordButton = findViewById(R.id.camera_recordbutton)
        recordButton.setOnClickListener {
            cameraView.sessionType = SessionType.VIDEO
            var file = File(this.filesDir, "vid2")
            cameraView.startCapturingVideo(file)

            pictureBar.animate().setDuration(200L).alpha(0f).setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    pictureBar.visibility = View.GONE
                }
            })
            recordBar.alpha = 0f
            recordBar.visibility = View.VISIBLE
            recordBar.animate().setDuration(200L).alpha(1f).setListener(null)

            var counter = 0
            recordTimer.scheduleAtFixedRate(object: TimerTask() {
                override fun run() {
                    var secs: String = (counter % 60).toString()
                    var mins: String = (counter/60).toString()
                    counter++
                    runOnUiThread {
                        durationText.text = mins + ":" + if (secs.length <= 1) {"0"} else {""} + secs
                    }
                }
            }, 0L, 1000L)
        }

        stopButton.setOnClickListener {
            cameraView.stopCapturingVideo()
            cameraView.sessionType = SessionType.PICTURE
            recordTimer.cancel()
            recordBar.animate().setDuration(200L).alpha(0f).setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    recordBar.visibility = View.GONE
                }
            })
            pictureBar.alpha = 0f
            pictureBar.visibility = View.VISIBLE
            pictureBar.animate().setDuration(200L).alpha(1f).setListener(null)
        }

        galleryButton = findViewById(R.id.camera_gallerybutton)
        switchCameraButton = findViewById(R.id.camera_switchcambutton)
        switchCameraButton.setOnClickListener {
            cameraView.toggleFacing()
            when (cameraView.facing) {
                Facing.FRONT -> {
                    switchCameraButton.setImageResource(R.drawable.camera_rear_white)
                }
                Facing.BACK -> {
                    switchCameraButton.setImageResource(R.drawable.camera_front_white)
                }
                else -> {
                    Log.d("STATE", "Camera was not front or back")
                }
            }
        }
        flashButton = findViewById(R.id.camera_flashbutton)
        flashButton.setOnClickListener {
            Log.d("STATE", cameraView.flash.toString())
            when (cameraView.flash) {
                Flash.AUTO -> {
                    flashButton.setImageResource(R.drawable.ic_flash_on_black_24dp)
                    cameraView.flash = Flash.ON
                }
                Flash.ON -> {
                    flashButton.setImageResource(R.drawable.ic_flash_off_black_24dp)
                    cameraView.flash = Flash.OFF
                }
                Flash.OFF -> {
                    flashButton.setImageResource(R.drawable.ic_flash_auto_black_24dp)
                    cameraView.flash = Flash.AUTO
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            cameraView.start()
        }
        else {
            ActivityCompat.requestPermissions(this, Array<String>(2, { i -> if (i == 0) {android.Manifest.permission.CAMERA} else { android.Manifest.permission.RECORD_AUDIO} }), REQUEST_CAMERA_PERMISSION)
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
