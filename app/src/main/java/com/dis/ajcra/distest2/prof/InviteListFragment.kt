package com.dis.ajcra.distest2.login

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amazonaws.services.sns.AmazonSNSClient
import com.dis.ajcra.distest2.DisGcmListener
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.SnsEvent
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class InviteListFragment : Fragment() {
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

    private lateinit var profile: MyProfile
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InviteRecyclerAdapter
    private lateinit var snsClient: AmazonSNSClient
    private var dataset: ArrayList<Invite> = ArrayList<Invite>()

    private lateinit var subLoginToken: String

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSnsEvent(evt: SnsEvent) {
        if (DisGcmListener.FRIEND_INVITE == evt.type) {
            var userInfo = evt.payload
            var invite = FriendInvite(profileManager.getProfile(userInfo), false)
            dataset.add(invite)
            adapter.notifyItemInserted(dataset.size - 1)
        } else if (DisGcmListener.FRIEND_REMOVED == evt.type) {
            var userId = evt.payload.getString("removerId")
            var i = 0
            while (i < dataset.size) {
                if (dataset[i].target.id == userId) {
                    dataset.removeAt(i)
                    adapter.notifyItemRemoved(i)
                    return
                }
            }
        } else if (DisGcmListener.FRIEND_ADDED == evt.type) {
            var userId = evt.payload.getString("id")
            var i = 0
            while (i < dataset.size) {
                if (dataset[i].target.id == userId) {
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
            if (ex != null) {
                async(UI) {
                    adapter.notifyItemRangeRemoved(0, dataset.size)
                    dataset.clear()
                    profile = profileManager.getMyProfile().await() as MyProfile
                    var invites = profile.getInvites().await()
                    for (invite in invites) {
                        dataset.add(invite)
                        adapter.notifyItemInserted(dataset.size - 1)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(context!!.applicationContext)
        profileManager = ProfileManager(cognitoManager, context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_invite_list, container, false)
        adapter = InviteRecyclerAdapter(cfm, dataset)
        return rootView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        if (rootView != null) {
            recyclerView = rootView.findViewById(R.id.invitelist_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true
        }
    }
}