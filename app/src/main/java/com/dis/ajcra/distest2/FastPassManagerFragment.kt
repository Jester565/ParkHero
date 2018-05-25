package com.dis.ajcra.distest2

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.login.EntityListFragment
import com.dis.ajcra.distest2.login.LoginActivity
import com.dis.ajcra.distest2.login.RegisterActivity
import com.dis.ajcra.distest2.prof.MyProfile
import com.dis.ajcra.distest2.prof.ProfileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import android.widget.RelativeLayout
import com.dis.ajcra.distest2.media.CloudFileManager

/*
class FastPassManagerFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var cfm: CloudFileManager
    private lateinit var rideManager: RideManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RideRecyclerAdapter
    private var dataset: ArrayList<Ride> = ArrayList<Ride>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context.applicationContext)
        adapter = RideRecyclerAdapter(cfm, dataset)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_ride_times, container, false)
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view != null) {
            recyclerView = view.findViewById(R.id.ridelist_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this@FastPassManagerFragment.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        async(UI) {
            var rides = rideManager.getRides().await()
            dataset.clear()
            for (ride in rides!!) {
                dataset.add(ride)
            }
            adapter.notifyDataSetChanged()
        }
    }
}
*/