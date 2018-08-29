package com.dis.ajcra.distest2.login

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dis.ajcra.distest2.DisGcmListener
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.SnsEvent
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.distest2.prof.ProfileItem
import com.dis.ajcra.distest2.prof.ProfileManager
import com.dis.ajcra.distest2.prof.ProfileRecyclerAdapter
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class FriendListFragment : Fragment() {
    companion object {
        var ID_PARAM = "id"
        var SELECTABLE_PARAM = "selectable"
        fun GetInstance(selectable: Boolean = false, id: String? = null): FriendListFragment {
            val fragment = FriendListFragment()
            val args = Bundle()
            if (id != null) {
                args.putString(ID_PARAM, id)
            }
            args.putBoolean(SELECTABLE_PARAM, selectable)
            fragment.arguments = args
            return fragment
        }
    }

    private var selectable: Boolean = false
    private lateinit var profile: Profile
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfileRecyclerAdapter
    private var dataset: ArrayList<ProfileItem> = ArrayList<ProfileItem>()

    private lateinit var subLoginToken: String

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSnsEvent(evt: SnsEvent) {
        if (DisGcmListener.FRIEND_ADDED == evt.type) {
            var userInfo = evt.payload
            var friend = ProfileItem(profileManager.getProfile(userInfo))
            dataset.add(friend)
            adapter.notifyItemInserted(dataset.size - 1)
        } else if (DisGcmListener.FRIEND_REMOVED == evt.type) {
            var userId = evt.payload.getString("removerId")
            var i = 0
            while (i < dataset.size) {
                if (dataset[i].profile.id == userId) {
                    dataset.removeAt(i)
                    adapter.notifyItemRemoved(i)
                    return
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()

        subLoginToken = cognitoManager.subscribeToLogin { ex ->
            if (ex == null) {
                async(UI) {
                    adapter.notifyItemRangeRemoved(0, dataset.size)
                    dataset.clear()
                    var friends = profile.getFriends().await()
                    for (friend in friends) {
                        dataset.add(ProfileItem(friend))
                        adapter.notifyItemInserted(dataset.size - 1)
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cognitoManager.unsubscribeFromLogin(subLoginToken)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        profileManager = ProfileManager(cognitoManager, this.context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_friend_list, container, false)
        adapter = ProfileRecyclerAdapter(cfm, dataset, selectable)
        return rootView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        if (rootView != null) {
            recyclerView = rootView.findViewById(R.id.friendlist_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true
        }
    }

    fun getSelected(): ArrayList<Profile> {
        var selectedProfiles = ArrayList<Profile>()
        dataset.forEach { it ->
            if (it.selected) {
                selectedProfiles.add(it.profile)
            }
        }
        return selectedProfiles
    }
}