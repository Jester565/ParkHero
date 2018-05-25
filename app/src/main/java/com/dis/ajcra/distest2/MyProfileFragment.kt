package com.dis.ajcra.distest2

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.MyProfile
import com.dis.ajcra.distest2.prof.ProfileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import java.lang.Exception


class MyProfileFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager
    private lateinit var myProfile: MyProfile

    private lateinit var profImg: ImageView
    private lateinit var nameText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_my_profile, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        profileManager = ProfileManager(cognitoManager)
        cfm = CloudFileManager.GetInstance(cognitoManager, context.applicationContext)
        return rootView
    }

    override fun onViewCreated(rootView: View?, savedInstanceState: Bundle?) {
        if (rootView != null) {
            profImg = rootView.findViewById(R.id.myprofile_profimg)
            nameText = rootView.findViewById(R.id.myprofile_name)

            async(UI) {
                myProfile = profileManager.getMyProfile().await() as MyProfile
                async {
                    var profilePicUrl = myProfile.getProfilePicUrl().await()
                    if (profilePicUrl == null) {
                        profilePicUrl = "profileImgs/blank-profile-picture-973460_640.png"
                    }
                    cfm.download(profilePicUrl, object : CloudFileListener() {
                        override fun onError(id: Int, ex: Exception?) {
                        }

                        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                        }

                        override fun onStateChanged(id: Int, state: TransferState?) {
                        }

                        override fun onComplete(id: Int, file: File) {
                            async {
                                var options = BitmapFactory.Options()
                                options.inJustDecodeBounds = true
                                BitmapFactory.decodeFile(file.absolutePath, options)
                                var dispMetrics = DisplayMetrics()
                                this@MyProfileFragment.activity.windowManager.defaultDisplay.getMetrics(dispMetrics)

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