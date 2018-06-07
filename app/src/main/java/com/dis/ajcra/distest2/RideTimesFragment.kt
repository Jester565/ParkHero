package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.experimental.async
import java.util.*


class RideTimesFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var cfm: CloudFileManager
    private lateinit var rideManager: RideManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RideRecyclerAdapter
    private var rides = ArrayList<CRInfo>()
    private var pinnedRides = ArrayList<CRInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context.applicationContext)
        async {
            cfm.displayFileInfo()
        }
        rideManager = RideManager(context.applicationContext)
        adapter = RideRecyclerAdapter(cfm, rides, pinnedRides)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_ride_times, container, false)
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view != null) {
            recyclerView = view.findViewById(R.id.ridelist_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this@RideTimesFragment.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(200)
            recyclerView.isDrawingCacheEnabled = true
        }
    }

    fun getRides() {
        rideManager.listRides(object: RideManager.ListRidesCB {
            override fun onAllUpdated(rides: ArrayList<CRInfo>) {
                //animateBackground()
            }

            override fun init(rideUpdates: ArrayList<CRInfo>) {
                if (rides.isEmpty() && pinnedRides.isEmpty()) {
                    for (ride in rideUpdates) {
                        if (ride.pinned) {
                            pinnedRides.add(ride)
                        } else {
                            rides.add(ride)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onAdd(ride: CRInfo) {
                if (ride.pinned) {
                    var arrI = pinnedRides.binarySearch {
                        it.name.compareTo(ride.name)
                    }
                    var insertI = -(arrI + 1)
                    pinnedRides.add(insertI, ride)
                    adapter.notifyItemInserted(insertI)
                } else {
                    var arrI = rides.binarySearch {
                        it.name.compareTo(ride.name)
                    }
                    var insertI = -(arrI + 1)
                    rides.add(insertI, ride)
                    adapter.notifyItemInserted(insertI + pinnedRides.size)
                }
            }

            override fun onUpdate(ride: CRInfo) {
                if (ride.pinned) {
                    var arrI = pinnedRides.binarySearch {
                        it.name.compareTo(ride.name)
                    }
                    if (arrI < 0) {
                        Log.e("ERRRRRR", "RideI was less than 0 on onUpdate")
                        return
                    }
                    pinnedRides[arrI] = ride
                    adapter.notifyItemChanged(arrI)
                } else {
                    var arrI = rides.binarySearch {
                        it.name.compareTo(ride.name)
                    }
                    if (arrI < 0) {
                        Log.e("ERRRRRR", "RideI was less than 0 on onUpdate")
                        return
                    }
                    rides[arrI] = ride
                    adapter.notifyItemChanged(arrI + pinnedRides.size)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        getRides()
    }
}
