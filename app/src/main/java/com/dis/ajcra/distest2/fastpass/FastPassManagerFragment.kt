package com.dis.ajcra.distest2.fastpass

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.dis.ajcra.distest2.AppSyncTest
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.ride.RideManager
import com.dis.ajcra.fastpass.fragment.DisFastPassTransaction
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async

class FastPassManagerFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    private lateinit var cognitoManager: CognitoManager
    private lateinit var rideManager: RideManager
    private lateinit var cfm: CloudFileManager
    private lateinit var appSync: AppSyncTest

    private lateinit var adapter: FastPassRecyclerAdapter
    private var dataset = ArrayList<DisFastPassTransaction>()

    private lateinit var subLoginToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
        appSync = AppSyncTest.GetInstance(cognitoManager, context!!.applicationContext)
        rideManager = RideManager.GetInstance(cognitoManager, context!!.applicationContext)
        adapter = FastPassRecyclerAdapter(dataset, rideManager, cfm)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val rootView = inflater!!.inflate(R.layout.fragment_fast_pass, container, false)
        recyclerView = rootView.findViewById(R.id.fastpass_recycler)
        emptyText = rootView.findViewById(R.id.fastpass_emptyText)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view != null) {
            var layoutManager = LinearLayoutManager(this@FastPassManagerFragment.context)
            layoutManager.orientation = LinearLayout.HORIZONTAL
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        subLoginToken = cognitoManager.subscribeToLogin { ex ->
            appSync.updateFastPasses(object: AppSyncTest.UpdateFastPassesCallback {
                override fun onResponse(response: List<DisFastPassTransaction>) {
                    async(UI) {
                        dataset.clear()
                        if (response.size > 0) {
                            dataset.addAll(response)
                            recyclerView.visibility = View.VISIBLE
                            emptyText.visibility = View.GONE
                        } else {
                            recyclerView.visibility = View.GONE
                            emptyText.visibility = View.VISIBLE
                        }
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onError(ec: Int?, msg: String?) { }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        cognitoManager.unsubscribeFromLogin(subLoginToken)
    }
}