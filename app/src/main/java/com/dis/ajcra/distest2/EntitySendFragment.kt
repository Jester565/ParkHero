package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.login.FriendListFragment
import com.dis.ajcra.distest2.login.LoginFragment
import com.dis.ajcra.distest2.prof.ProfileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async

class EntitySendFragment: DialogFragment() {
    private lateinit var profileManager: ProfileManager
    private lateinit var objKeys: ArrayList<String>
    private lateinit var sendButton: ImageButton
    private lateinit var friendListFragment: FriendListFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        objKeys = arguments.getStringArrayList(OBJKEYS_PARAM)
        var cognitoManager = CognitoManager.GetInstance(context.applicationContext)
        profileManager = ProfileManager(cognitoManager)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_entity_send, container)
        dialog.setTitle("Send")
        friendListFragment = FriendListFragment.GetInstance(true)
        childFragmentManager.beginTransaction().add(R.id.entitysend_friendholder, friendListFragment).commit()
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        if (view != null) {
            sendButton = view.findViewById(R.id.entitysend_sendbutton)
            sendButton.setOnClickListener {
                sendButton.isEnabled = false
                async(UI) {
                    var selectedProfiles = friendListFragment.getSelected()
                    objKeys.forEach { it ->
                        profileManager.sendEntity(it, selectedProfiles).await()
                    }
                    this@EntitySendFragment.dismiss()
                }
            }
        }
    }

    companion object {
        var OBJKEYS_PARAM = "objKeys"
        fun GetInstance(objKeys: ArrayList<String>): EntitySendFragment {
            val fragment = EntitySendFragment()
            val args = Bundle()
            args.putStringArrayList(OBJKEYS_PARAM, objKeys)
            fragment.arguments = args
            return fragment
        }
    }
}