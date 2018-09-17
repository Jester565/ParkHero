package com.dis.ajcra.distest2.prof

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.camera.CameraFragment
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.pass.RenameProfileFragment
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import java.net.URI




class MyProfileFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager
    private lateinit var myProfile: MyProfile

    private lateinit var profileLayout: RelativeLayout
    private lateinit var cameraLayout: RelativeLayout

    private lateinit var profImg: ImageView
    private lateinit var nameText: TextView

    private lateinit var editProfImg: ImageButton
    private lateinit var editProfName: ImageButton

    private lateinit var subLoginToken: String

    private var cameraFragment: CameraFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_my_profile, container, false)
        cognitoManager = CognitoManager.GetInstance(context!!.applicationContext)
        profileManager = ProfileManager(cognitoManager, context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
        return rootView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        if (rootView != null) {
            profileLayout = rootView.findViewById(R.id.myprofile_profileHolder)
            cameraLayout = rootView.findViewById(R.id.myprofile_cameraHolder)
            profImg = rootView.findViewById(R.id.myprofile_profimg)
            editProfImg = rootView.findViewById(R.id.myprofile_imgEdit)
            nameText = rootView.findViewById(R.id.myprofile_name)
            editProfName = rootView.findViewById(R.id.myprofile_nameEdit)
        }
    }

    override fun onResume() {
        super.onResume()
        subLoginToken = cognitoManager.subscribeToLogin { ex ->
            if (ex == null) {
                async(UI) {
                    myProfile = profileManager.getMyProfile().await() as MyProfile
                    async {
                        var profilePicUrl = myProfile.getProfilePicUrl().await()
                        if (profilePicUrl == null) {
                            profilePicUrl = "profileImgs/blank-profile-picture-973460_640.png"
                        }
                        cfm.download(profilePicUrl, object : CloudFileListener() {
                            override fun onComplete(id: Int, file: File) {
                                async {
                                    var options = BitmapFactory.Options()
                                    options.inJustDecodeBounds = true
                                    BitmapFactory.decodeFile(file.absolutePath, options)
                                    var dispMetrics = DisplayMetrics()
                                    this@MyProfileFragment.activity!!.windowManager.defaultDisplay.getMetrics(dispMetrics)

                                    var imgScale = 1

                                    while (options.outWidth / imgScale > dispMetrics.widthPixels) {
                                        imgScale *= 2
                                    }
                                    options.inJustDecodeBounds = false
                                    options.inSampleSize = imgScale
                                    var bmap = BitmapFactory.decodeFile(file.absolutePath, options)
                                    async(UI) {
                                        profImg.setImageBitmap(bmap)
                                    }
                                }
                            }
                        }, null, true)
                    }
                    editProfImg.setOnClickListener {
                        cameraFragment = CameraFragment()
                        cameraFragment!!.setOnPhotoCallback { file ->
                            Log.d("STATE", "PHOTO CALLBACK")
                            CropImage.activity(Uri.fromFile(file))
                                    .setAspectRatio(7, 7)
                                    .start(context!!, this@MyProfileFragment)
                        }
                        cameraFragment!!.arguments = Bundle()
                        cameraFragment!!.arguments!!.putBoolean("videoEnabled", false)
                        cameraFragment!!.arguments!!.putBoolean("galleryEnabled", false)
                        var transaction = fragmentManager!!.beginTransaction()
                        transaction.replace(R.id.myprofile_cameraHolder, cameraFragment).commit()
                        profileLayout.visibility = View.GONE
                        cameraLayout.visibility = View.VISIBLE
                    }
                    nameText.setText(myProfile.getName().await())
                    editProfName.setOnClickListener {
                        var esf = RenameProfileFragment.GetInstance(nameText.text.toString())
                        esf.setOnRenameCallback { newName ->
                            nameText.setText(newName)
                        }
                        esf.show(fragmentManager, "Entity Send")
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri = result.uri
                async {
                    var objKey = "tmpProfileImgs/" + cognitoManager.federatedID + ".jpg"
                    cfm.upload(objKey, URI(result.uri.toString()), object : CloudFileListener() {})
                }
                removeCameraFragment()
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && cameraFragment != null) {
            removeCameraFragment()
            return true
        }
        return false
    }

    fun removeCameraFragment() {
        var transaction = fragmentManager!!.beginTransaction()
        transaction.remove(cameraFragment).commit()
        cameraLayout.visibility = View.GONE
        profileLayout.visibility = View.VISIBLE
        cameraFragment = null
    }

    override fun onPause() {
        super.onPause()
        cognitoManager.unsubscribeFromLogin(subLoginToken)
    }
}