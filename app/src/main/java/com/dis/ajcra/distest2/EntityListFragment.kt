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
import com.dis.ajcra.distest2.login.InviteListFragment.Companion.ID_PARAM
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.model.EntityInfo
import com.dis.ajcra.distest2.prof.MyProfile
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.distest2.prof.ProfileManager
import com.google.android.gms.tasks.Tasks.await
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.lang.Exception

class EntityListFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EntityRecyclerAdapter
    private var dataset: ArrayList<Entity> = ArrayList<Entity>()

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSnsEvent(evt: SnsEvent) {
        if (DisGcmListener.ENTITY_SENT == evt.type) {
            var entity = Entity(profileManager, evt.payload)
            dataset.add(entity)
            adapter.notifyItemInserted(dataset.size - 1)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        async(UI) {
            adapter.notifyItemRangeRemoved(0, dataset.size)
            dataset.clear()
            var entities = profileManager.getEntities().await()
            entities.forEach { it ->
                dataset.add(it)
                adapter.notifyItemInserted(dataset.size - 1)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        profileManager = ProfileManager(cognitoManager)
        cfm = CloudFileManager.GetInstance(cognitoManager, context.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_friend_list, container, false)
        adapter = EntityRecyclerAdapter(cfm, dataset)
        return rootView
    }

    override fun onViewCreated(rootView: View?, savedInstanceState: Bundle?) {
        if (rootView != null) {
            recyclerView = rootView.findViewById(R.id.friendlist_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true
            recyclerView.isNestedScrollingEnabled = false
        }
    }
}