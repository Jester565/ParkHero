package com.dis.ajcra.distest2.prof

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.dis.ajcra.distest2.DisGcmListener
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.SnsEvent
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class ProfileFragment : Fragment() {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSnsEvent(evt: SnsEvent) {
        if (DisGcmListener.FRIEND_INVITE == evt.type) {
            var userInfo = evt.payload
            var profile = profileManager.getProfile(userInfo)
            if (profile.id == this.profile.id) {
                initFriendStatus(2)
            }
        } else if (DisGcmListener.FRIEND_REMOVED == evt.type) {
            var userId = evt.payload.getString("removerId")
            if (userId == profile.id) {
                initFriendStatus(0)
            }
        } else if (DisGcmListener.FRIEND_ADDED == evt.type) {
            var userId = evt.payload.getString("id")
            if (userId == profile.id) {
                initFriendStatus(3)
            }
        } else if (DisGcmListener.PARTY_INVITE == evt.type) {
            var userId = evt.payload.getString("id")
            if (userId == profile.id) {
                initPartyStatus(2)
            }
        }
    }

    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager
    private lateinit var profile: Profile

    private lateinit var profImg: ImageView
    private lateinit var nameText: TextView
    private lateinit var acceptButton: Button
    private lateinit var declineButton: Button

    private lateinit var subLoginToken: String

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_profile, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        profileManager = ProfileManager(cognitoManager, this.context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
        profile = profileManager.getProfile(arguments!!.getString(ID_PARAM))
        return rootView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        if (rootView != null) {
            profImg = rootView.findViewById(R.id.profile_profimg)
            nameText = rootView.findViewById(R.id.profile_name)
            acceptButton = rootView.findViewById(R.id.profile_acceptButton)
            declineButton = rootView.findViewById(R.id.profile_declineButton)
        }
    }

    override fun onResume() {
        super.onResume()
        subLoginToken = cognitoManager.subscribeToLogin { ex ->
            if (ex == null) {
                GlobalScope.launch(Dispatchers.IO) {
                    var profilePicUrl = profile.getProfilePicUrl().await()
                    if (profilePicUrl == null) {
                        profilePicUrl = "profileImgs/blank-profile-picture-973460_640.png"
                    }
                    cfm.download(profilePicUrl, object : CloudFileListener() {
                        override fun onComplete(id: Int, file: File) {
                            async(Dispatchers.IO) {
                                var options = BitmapFactory.Options()
                                options.inJustDecodeBounds = true
                                BitmapFactory.decodeFile(file.absolutePath, options)
                                var dispMetrics = DisplayMetrics()
                                this@ProfileFragment.activity!!.windowManager.defaultDisplay.getMetrics(dispMetrics)

                                var imgScale = 1

                                while (options.outWidth / imgScale > dispMetrics.widthPixels) {
                                    imgScale *= 2
                                }
                                options.inJustDecodeBounds = false
                                options.inSampleSize = imgScale
                                var bmap = BitmapFactory.decodeFile(file.absolutePath, options)
                                launch(Dispatchers.Main) {
                                    profImg.setImageBitmap(bmap)
                                }
                            }
                        }
                    }, null, true)
                }
                GlobalScope.launch(Dispatchers.Main) {
                    nameText.setText(profile.getName().await())
                    var status = profile.getInviteStatus().await()
                    var type = profile.getInviteType().await()
                    if (type == 0) {
                        initFriendStatus(status)
                    } else {
                        initPartyStatus(status)
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cognitoManager.unsubscribeFromLogin(subLoginToken)
    }

    fun initFriendStatus(status: Int) {
        acceptButton.isEnabled = true
        declineButton.isEnabled = true
        acceptButton.visibility = View.GONE
        declineButton.visibility = View.GONE

        if (status == 0) {
            //NOT A FRIEND, NO INVITES
            acceptButton.visibility = View.VISIBLE
            acceptButton.text = "Send Friend Request"
            acceptButton.setOnClickListener {
                acceptButton.isEnabled = false
                GlobalScope.launch(Dispatchers.Main) {
                    var nowFriend = profile.addFriend().await()
                    if (nowFriend) {
                        initFriendStatus(3)
                    } else {
                        initFriendStatus(1)
                    }
                }
            }
        } else if (status == 1) {
            //NOT A FRIEND, YOU SENT INVITE
            declineButton.visibility = View.VISIBLE
            declineButton.text = "Cancel Friend Request"
            declineButton.setOnClickListener {
                declineButton.isEnabled = false
                GlobalScope.launch(Dispatchers.Main) {
                    profile.removeFriend().await()
                    initFriendStatus(0)
                }
            }
        } else if (status == 2) {
            //NOT A FRIEND, YOU RECEIVED INVITE
            acceptButton.visibility = View.VISIBLE
            declineButton.visibility = View.VISIBLE
            acceptButton.text = "Accept Friend Request"
            declineButton.text = "Decline Friend Request"
            acceptButton.setOnClickListener {
                acceptButton.isEnabled = false
                declineButton.isEnabled = false
                GlobalScope.launch(Dispatchers.Main) {
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
                GlobalScope.launch(Dispatchers.Main) {
                    profile.removeFriend().await()
                    initFriendStatus(0)
                }
            }
        } else {
            acceptButton.visibility = View.VISIBLE
            acceptButton.text = "Invite to Party"
            acceptButton.setOnClickListener {
                acceptButton.isEnabled = false
                declineButton.isEnabled = false
                GlobalScope.launch(Dispatchers.Main) {
                    profile.addToParty().await()
                    initPartyStatus(1)
                }
            }
            declineButton.visibility = View.VISIBLE
            declineButton.text = "Unfriend"
            declineButton.setOnClickListener {
                declineButton.isEnabled = false
                GlobalScope.launch(Dispatchers.Main) {
                    profile.removeFriend().await()
                    initFriendStatus(0)
                }
            }
        }
    }

    fun initPartyStatus(status: Int) {
        acceptButton.isEnabled = true
        declineButton.isEnabled = true
        acceptButton.visibility = View.GONE
        declineButton.visibility = View.GONE
        if (status == 1) {
            //YOU SEND A PARTY INVITE
            declineButton.visibility = View.VISIBLE
            declineButton.text = "Cancel Party Invite"
            declineButton.setOnClickListener {
                declineButton.isEnabled = false
                GlobalScope.launch(Dispatchers.Main) {
                    profile.removePartyInvite().await()
                    initFriendStatus(3)
                }
            }
        } else if (status == 2) {
            //NOT A FRIEND, YOU RECEIVED INVITE
            acceptButton.visibility = View.VISIBLE
            declineButton.visibility = View.VISIBLE
            acceptButton.text = "Accept Party Invite"
            declineButton.text = "Decline Party Invite"
            acceptButton.setOnClickListener {
                acceptButton.isEnabled = false
                declineButton.isEnabled = false
                GlobalScope.launch(Dispatchers.Main) {
                    profile.addToParty().await()
                    initFriendStatus(3)
                }
            }

            declineButton.setOnClickListener {
                acceptButton.isEnabled = false
                declineButton.isEnabled = false
                GlobalScope.launch(Dispatchers.Main) {
                    profile.removePartyInvite().await()
                    initFriendStatus(3)
                }
            }
        }
    }
}