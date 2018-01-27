package com.dis.ajcra.distest2.login

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.*
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.distest2.prof.ProfileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import java.lang.Exception

class FriendListFragment : Fragment() {
    private lateinit var profile: Profile
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfileRecyclerAdapter
    private var dataset: ArrayList<Profile> = ArrayList<Profile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        profileManager = ProfileManager(cognitoManager)
        cfm = CloudFileManager.GetInstance(cognitoManager.credentialsProvider, context.applicationContext)
        if (arguments != null && arguments.getString(ID_PARAM) != null) {
            profile = profileManager.getProfile(arguments.getString(ID_PARAM))
        } else {
            profile = profileManager.getProfile(cognitoManager.federatedID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_friend_list, container, false)
        adapter = ProfileRecyclerAdapter(cfm, dataset)
        return rootView
    }

    override fun onViewCreated(rootView: View?, savedInstanceState: Bundle?) {
        if (rootView != null) {
            recyclerView = rootView.findViewById(R.id.friendlist_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true
            async(UI) {
                var friends = profile.getFriends().await()
                for (friend in friends) {
                    dataset.add(friend)
                    adapter.notifyItemInserted(dataset.size - 1)
                }
            }
        }
    }

    companion object {
        var ID_PARAM = "id"
        fun GetInstance(id: String? = null): FriendListFragment {
            val fragment = FriendListFragment()
            if (id != null) {
                val args = Bundle()
                args.putString(ID_PARAM, id)
                fragment.arguments = args
            }
            return fragment
        }
    }
}