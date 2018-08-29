package com.dis.ajcra.distest2.prof

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File

class MyProfileFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager
    private lateinit var myProfile: MyProfile

    private lateinit var profImg: ImageView
    private lateinit var nameText: TextView

    private lateinit var subLoginToken: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_my_profile, container, false)
        cognitoManager = CognitoManager.GetInstance(context!!.applicationContext)
        profileManager = ProfileManager(cognitoManager, context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
        return rootView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        if (rootView != null) {
            profImg = rootView.findViewById(R.id.myprofile_profimg)
            nameText = rootView.findViewById(R.id.myprofile_name)
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
                    profImg.setOnClickListener {
                        //var intent = Intent(this@MyProfileFragment.context, ProfilePicSelection::class.java)
                        //startActivity(intent)
                    }

                    nameText.setText(myProfile.getName().await())
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cognitoManager.unsubscribeFromLogin(subLoginToken)
    }
}