package com.dis.ajcra.distest2.entity

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.login.FriendListFragment
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.distest2.prof.ProfileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async

class SendFragment: DialogFragment() {
    companion object {
        var OBJKEYS_PARAM = "objKeys"
        fun GetInstance(): SendFragment {
            val fragment = SendFragment()
            return fragment
        }
    }

    private lateinit var profileManager: ProfileManager
    private lateinit var sendButton: ImageButton
    private lateinit var friendListFragment: FriendListFragment

    var sendCallback: (suspend (HashSet<Profile>) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var cognitoManager = CognitoManager.GetInstance(context!!.applicationContext)
        profileManager = ProfileManager(cognitoManager, context!!.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_send, container)
        dialog.setTitle("Send")
        friendListFragment = FriendListFragment.GetInstance(true, true)
        friendListFragment.setSelectCallback { profiles ->
            if (profiles.size > 0) {
                sendButton.visibility = View.VISIBLE
            } else {
                sendButton.visibility = View.GONE
            }
        }
        childFragmentManager.beginTransaction().add(R.id.entitysend_friendholder, friendListFragment).commit()
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (view != null) {
            sendButton = view.findViewById(R.id.entitysend_sendbutton)
            sendButton.setOnClickListener {
                async(UI) {
                    sendButton.isEnabled = false
                    var dismiss = sendCallback?.invoke(friendListFragment.getSelected()!!)
                    if (dismiss != null && dismiss) {
                        this@SendFragment.dismiss()
                    }
                    sendButton.isEnabled = true
                }
            }
        }
    }
}