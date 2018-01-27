package com.dis.ajcra.distest2

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.distest2.prof.ProfileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import java.lang.Exception


class ProfileFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager
    private lateinit var profile: Profile

    private lateinit var profImg: ImageView
    private lateinit var nameText: TextView
    private lateinit var acceptButton: Button
    private lateinit var declineButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_profile, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        profileManager = ProfileManager(cognitoManager)
        cfm = CloudFileManager.GetInstance(cognitoManager.credentialsProvider, context.applicationContext)
        profile = profileManager.getProfile(arguments.getString(ID_PARAM))
        return rootView
    }

    override fun onViewCreated(rootView: View?, savedInstanceState: Bundle?) {
        if (rootView != null) {
            profImg = rootView.findViewById(R.id.profile_profimg)
            nameText = rootView.findViewById(R.id.profile_name)
            acceptButton = rootView.findViewById(R.id.profile_acceptButton)
            declineButton = rootView.findViewById(R.id.profile_declineButton)

            async {
                var profilePicUrl = profile.getProfilePicUrl().await()
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
                            this@ProfileFragment.activity.windowManager.defaultDisplay.getMetrics(dispMetrics)

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
                })
            }
            async(UI) {
                nameText.setText(profile.getName().await())
                var status = profile.getInviteStatus().await()
                initFriendStatus(status)
            }
        }
    }

    fun initFriendStatus(status: Int) {
        acceptButton.isEnabled = true
        acceptButton.visibility = View.GONE
        declineButton.visibility = View.GONE

        if (status == 0) {
            acceptButton.visibility = View.VISIBLE
            acceptButton.text = "Send Friend Request"
            acceptButton.setOnClickListener {
                acceptButton.isEnabled = false
                async(UI) {
                    var nowFriend = profile.addFriend().await()
                    if (nowFriend) {
                        initFriendStatus(3)
                    } else {
                        initFriendStatus(1)
                    }
                }
            }
        } else if (status == 1) {
            declineButton.visibility = View.VISIBLE
            declineButton.text = "Cancel Friend Request"
            declineButton.setOnClickListener {
                declineButton.isEnabled = false
                async(UI) {
                    profile.removeFriend().await()
                    initFriendStatus(0)
                }
            }
        } else if (status == 2) {
            acceptButton.visibility = View.VISIBLE
            declineButton.visibility = View.VISIBLE
            acceptButton.text = "Accept Friend Request"
            declineButton.text = "Decline Friend Request"
            acceptButton.setOnClickListener {
                acceptButton.isEnabled = false
                declineButton.isEnabled = false
                async(UI) {
                    var nowFriend = profile.addFriend().await()
                    if (nowFriend) {
                        initFriendStatus(3)
                    } else {
                        initFriendStatus(1)
                    }
                }
            }

            declineButton.setOnClickListener {
                acceptButton.isEnabled = false
                declineButton.isEnabled = false
                async(UI) {
                    profile.removeFriend().await()
                    initFriendStatus(0)
                }
            }
        } else {

        }
    }

    companion object {
        private val ID_PARAM = "id"

        fun newInstance(id: String): ProfileFragment {
            val fragment = ProfileFragment()
            val args = Bundle()
            args.putString(ID_PARAM, id)
            fragment.arguments = args
            return fragment
        }
    }
}