package com.dis.ajcra.distest2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.media.GalleryFragment
import com.dis.ajcra.distest2.media.GalleryFragmentListener
import com.otaliastudios.cameraview.*
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import id.zelory.compressor.Compressor
import kotlinx.coroutines.experimental.async
import java.io.File
import java.util.*

class CameraFragment : Fragment(), GalleryFragmentListener {
    companion object {
        val REQUEST_CAMERA_PERMISSION = 1
        val PICTURE_QUALITY = 60
        val VIDEO_QUALITY = VideoQuality.MAX_720P
        val BAR_TRANSITION_MILLIS = 400L
        val SLIDER_ALPHA = 0.7f
    }

    //Get picture taken and recorded events
    class DlCameraListener: CameraListener {

        private var cognitoManager: CognitoManager
        //private var transferUtil: TransferUtility
        private var appCtx: Context
        private var cloudFileManager: CloudFileManager
        private var compressor: Compressor
        private var galleryFragment: GalleryFragment
        private var visionHandler: VisionHandler
        constructor(cognitoManager: CognitoManager, galleryFragment: GalleryFragment, appContext: Context, activity: Activity) {
            this.cognitoManager = cognitoManager
            this.galleryFragment = galleryFragment
            this.appCtx = appContext
            cloudFileManager = CloudFileManager.GetInstance(cognitoManager, appContext)
            compressor = Compressor(appContext)
            compressor.setCompressFormat(Bitmap.CompressFormat.JPEG)
            compressor.setQuality(PICTURE_QUALITY)
            visionHandler = VisionHandler(activity)
        }

        override fun onCameraOpened(options: CameraOptions?) {
            super.onCameraOpened(options)
        }

        override fun onPictureTaken(jpeg: ByteArray?) {
            async {
                /*
                if (jpeg != null) {
                    var bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                    visionHandler.processBmp(bmp)
                }
                */
                //Save jpeg byte array to file
                var jpegFile = File(appCtx.filesDir, UUID.randomUUID().toString() + ".jpg")
                if (jpeg != null) {
                    jpegFile.writeBytes(jpeg)
                }
                var compFile = compressor.compressToFile(jpegFile)
                var objKey = "media/" + cognitoManager.federatedID + "/" + compFile.name
                Log.d("STATE", "File Name: " + jpegFile.name)
                Log.d("STATE", "File Len: " + jpegFile.length())
                Log.d("STATE", "ObjKey: " + objKey)
                visionHandler.processFile(appCtx, Uri.fromFile(compFile))
                /*
                var transferObserver = transferUtil.upload("disneyapp", objKey, jpegFile)
                if (transferListener != null) {
                    transferObserver.setTransferListener(transferListener)
                }
                */
                Log.d("FI", "PRE GEN CFI:")
                cloudFileManager.displayFileInfo()
                Log.d("FI", "PRE UPLOAD:")
                cloudFileManager.displayFileInfo()

                cloudFileManager.upload(objKey, compFile.toURI(), object: CloudFileListener() {
                    override fun onError(id: Int, ex: Exception?) {
                        Log.d("STATE", "On error: " + ex)
                    }

                    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

                    }

                    override fun onStateChanged(id: Int, state: TransferState?) {
                        Log.d("STATE", "State changed " + state)
                    }

                    override fun onComplete(id: Int, file: File) {
                        Log.d("STATE", "On Complete")
                    }
                })
                //Log.d("STATE", "Gallery Updated")
                galleryFragment.galleryUpdated(objKey)
            }
        }

        override fun onVideoTaken(file: File?) {
            if (file != null) {
                Log.d("STATE", "Uploading video")
                async {
                    var objKey = "media/" + cognitoManager.federatedID + "/" + file.name
                    cloudFileManager.upload(objKey, file.toURI(), object : CloudFileListener() {
                        override fun onError(id: Int, ex: Exception?) {

                        }

                        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

                        }

                        override fun onStateChanged(id: Int, state: TransferState?) {

                        }

                        override fun onComplete(id: Int, file: File) {
                            Log.d("STATE", "VIDEO UPLOADED")
                        }
                    })
                    galleryFragment.galleryUpdated(objKey)
                }
            }
        }
    }

    private lateinit var cameraView: CameraView
    private lateinit var pictureButton: FloatingActionButton
    private lateinit var recordButton: FloatingActionButton
    private lateinit var galleryButton: FloatingActionButton
    private lateinit var switchCameraButton: ImageButton
    private lateinit var flashButton: ImageButton
    private lateinit var pictureBar: RelativeLayout
    private lateinit var recordBar: RelativeLayout
    private lateinit var durationText: TextView
    private lateinit var stopButton: FloatingActionButton
    private lateinit var gallerySlider: SlidingUpPanelLayout
    private lateinit var galleryLayout: LinearLayout
    private lateinit var galleryFragment: GalleryFragment
    private var recordTimer: Timer = Timer()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_camera, container, false)

        cameraView = rootView.findViewById(R.id.camera_camera)
        //Camera Options
        cameraView.mapGesture(Gesture.SCROLL_VERTICAL, GestureAction.ZOOM)
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER)
        cameraView.videoQuality = VIDEO_QUALITY

        pictureBar = rootView.findViewById(R.id.camera_lowbar)
        recordBar = rootView.findViewById(R.id.camera_recordlowbar)
        durationText = rootView.findViewById(R.id.camera_durationtext)
        stopButton = rootView.findViewById(R.id.camera_stopbutton)

        pictureButton = rootView.findViewById(R.id.camera_picturebutton)
        pictureButton.setOnClickListener {
            cameraView.capturePicture()
        }

        recordButton = rootView.findViewById(R.id.camera_recordbutton)
        recordButton.setOnClickListener {
            //Set camera to take video
            cameraView.sessionType = SessionType.VIDEO
            //Provide file the recorded video should go to
            var file = File(activity.filesDir, UUID.randomUUID().toString() + ".mp4")
            cameraView.startCapturingVideo(file)

            //Show the record bar
            pictureBar.animate().setDuration(BAR_TRANSITION_MILLIS).alpha(0f).setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    pictureBar.visibility = View.GONE
                }
            })
            recordBar.alpha = 0f
            recordBar.visibility = View.VISIBLE
            recordBar.animate().setDuration(BAR_TRANSITION_MILLIS).alpha(1f).setListener(null)

            var recordSeconds = 0
            recordTimer.scheduleAtFixedRate(object: TimerTask() {
                override fun run() {
                    var secs: String = (recordSeconds % 60).toString()
                    var mins: String = (recordSeconds/60).toString()
                    recordSeconds++
                    activity.runOnUiThread {
                        durationText.text = mins + ":" + if (secs.length <= 1) {"0"} else {""} + secs
                    }
                }
            }, 0L, 1000L)
        }

        //Show picture bar
        stopButton.setOnClickListener {
            cameraView.stopCapturingVideo()
            cameraView.sessionType = SessionType.PICTURE
            recordTimer.cancel()
            recordBar.animate().setDuration(BAR_TRANSITION_MILLIS).alpha(0f).setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    recordBar.visibility = View.GONE
                }
            })
            pictureBar.alpha = 0f
            pictureBar.visibility = View.VISIBLE
            pictureBar.animate().setDuration(BAR_TRANSITION_MILLIS).alpha(1f).setListener(null)
        }

        galleryButton = rootView.findViewById(R.id.camera_gallerybutton)
        switchCameraButton = rootView.findViewById(R.id.camera_switchcambutton)
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
        flashButton = rootView.findViewById(R.id.camera_flashbutton)
        flashButton.setOnClickListener {
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

        galleryLayout = rootView.findViewById(R.id.camera_gallerylayout)
        galleryLayout.alpha = SLIDER_ALPHA
        gallerySlider = rootView.findViewById(R.id.camera_galleryslider)
        gallerySlider.addPanelSlideListener(object: SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                when (cameraView.sessionType) {
                    SessionType.PICTURE -> {
                        pictureBar.alpha = (1.0f - slideOffset)
                    }
                    SessionType.VIDEO -> {
                        recordBar.alpha = (1.0f - slideOffset)
                    }
                }
            }

            override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
                when (newState) {
                    SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                        when (cameraView.sessionType) {
                            SessionType.PICTURE -> {
                                pictureBar.visibility = View.VISIBLE
                                pictureBar.alpha = 1.0f
                            }
                            SessionType.VIDEO -> {
                                recordBar.visibility = View.VISIBLE
                                recordBar.alpha = 1.0f
                            }
                        }
                        galleryLayout.alpha = SLIDER_ALPHA
                    }
                    SlidingUpPanelLayout.PanelState.DRAGGING -> {
                        galleryFragment.setDrag(false)
                        when (cameraView.sessionType) {
                            SessionType.PICTURE -> {
                                pictureBar.visibility = View.VISIBLE
                            }
                            SessionType.VIDEO -> {
                                recordBar.visibility = View.VISIBLE
                            }
                        }
                        galleryLayout.alpha = 1.0f
                    }
                    SlidingUpPanelLayout.PanelState.EXPANDED -> {
                        galleryFragment.setDrag(true)
                        when (cameraView.sessionType) {
                            SessionType.PICTURE -> {
                                pictureBar.visibility = View.GONE
                                pictureBar.alpha = 0.0f
                            }
                            SessionType.VIDEO -> {
                                recordBar.visibility = View.GONE
                                recordBar.alpha = 0.0f
                            }
                        }
                        galleryLayout.alpha = 1.0f
                    }
                }
            }
        })
        return rootView
    }

    override fun onRecyclerViewCreated(recyclerView: RecyclerView) {
        gallerySlider.setScrollableView(recyclerView)
    }

    override fun onDragChange(dragging: Boolean) {

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //Add gallery fragment
        galleryFragment = GalleryFragment()
        var transaction = childFragmentManager.beginTransaction()
        transaction.add(R.id.camera_gallerylayout, galleryFragment).commit()
        var cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        cameraView.addCameraListener(DlCameraListener(cognitoManager, galleryFragment, activity.applicationContext, activity))
    }

    //Called when the activity starts or redisplayed
    override fun onResume() {
        super.onResume()
        //Check for Camera and Audio permissions needed for CameraView
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(activity, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            cameraView.start()
        }
        else {
            //Initialize array of permissions
            ActivityCompat.requestPermissions(activity, Array<String>(2, { i -> if (i == 0) {android.Manifest.permission.CAMERA} else { android.Manifest.permission.RECORD_AUDIO} }), REQUEST_CAMERA_PERMISSION)
        }
    }

    //Called when user selects permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (permissions.isEmpty() || grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //When activity is no longer displayed
    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }
}
