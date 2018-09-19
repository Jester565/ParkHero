package com.dis.ajcra.distest2.prof

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.dis.ajcra.distest2.DisGcmListener
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.SnsEvent
import com.dis.ajcra.distest2.entity.SendFragment
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PartyFragment : Fragment() {
    companion object {
        fun GetInstance(): PartyFragment {
            val fragment = PartyFragment()
            return fragment
        }
    }

    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager

    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageText: TextView
    private lateinit var inviteButton: Button
    private lateinit var leavePartyButton: Button
    private lateinit var adapter: ProfileRecyclerAdapter
    private var dataset = ArrayList<Profile>()

    private lateinit var subLoginToken: String

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSnsEvent(evt: SnsEvent) {
        if (DisGcmListener.FRIEND_ADDED == evt.type) {
            var userInfo = evt.payload
            dataset.add(profileManager.getProfile(userInfo))
            adapter.notifyItemInserted(dataset.size - 1)
        } else if (DisGcmListener.FRIEND_REMOVED == evt.type) {
            var userId = evt.payload.getString("removerId")
            var i = 0
            while (i < dataset.size) {
                if (dataset[i].id == userId) {
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
                updateParty()
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
        adapter = ProfileRecyclerAdapter(cfm, dataset, false, false, true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_party, container, false)
        return rootView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        if (rootView != null) {
            progressBar = rootView.findViewById(R.id.party_progress)
            recyclerView = rootView.findViewById(R.id.party_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true
            messageText = rootView.findViewById(R.id.party_message)
            inviteButton = rootView.findViewById(R.id.party_inviteButton)
            leavePartyButton = rootView.findViewById(R.id.party_leaveButton)

            inviteButton.setOnClickListener {
                var esf = SendFragment.GetInstance()
                esf.sendCallback = { profiles ->
                    profiles.forEach {
                        it.addToParty()
                    }
                    true
                }
                esf.show(childFragmentManager, "Invite To Party")
            }

            leavePartyButton.setOnClickListener {
                async(UI) {
                    inviteButton.isEnabled = false
                    leavePartyButton.isEnabled = false
                    profileManager.leaveParty().await()
                    updateParty()
                }
            }
        }
    }

    fun updateParty() = async(UI) {
        try {
            var partyProfiles = profileManager.getPartyProfiles().await()
            dataset.clear()
            if (partyProfiles.size > 1) {
                dataset.addAll(partyProfiles)
            }
        } catch (ex: Exception) {
            Log.d("STATE", "PARTYPROF EX: " + ex)
            dataset.clear()
        }
        adapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
        inviteButton.visibility = View.VISIBLE
        if (!dataset.isEmpty()) {
            recyclerView.visibility = View.VISIBLE
            messageText.visibility = View.GONE
            leavePartyButton.visibility = View.VISIBLE
            inviteButton.text = "Invite to Party"
            inviteButton.isEnabled = true
            leavePartyButton.isEnabled = true
        } else {
            recyclerView.visibility = View.GONE
            messageText.visibility = View.VISIBLE
            leavePartyButton.visibility = View.GONE
            inviteButton.text = "Create Party"
            inviteButton.isEnabled = true
        }
    }
}