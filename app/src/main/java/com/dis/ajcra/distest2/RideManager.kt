package com.dis.ajcra.distest2

import android.content.Context
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.dis.ajcra.fastpass.fragment.DisRide
import com.dis.ajcra.fastpass.fragment.DisRideUpdate
import kotlin.coroutines.experimental.suspendCoroutine


class RideManager {
    companion object {
        fun GetInstance(ctx: Context): RideManager {
            if (rideManager == null) {
                rideManager = RideManager(ctx)
            }
            return rideManager!!
        }

        private var rideManager: RideManager? = null
    }

    var appSync: AppSyncTest

    constructor(ctx: Context) {
        appSync = AppSyncTest.getInstance(ctx)
    }

    fun getRides(cb: AppSyncTest.GetRidesCallback) {
        appSync.getRides(cb)
    }

    fun getRideUpdates(cb: AppSyncTest.UpdateRidesCallback) {
        appSync.updateRides(cb)
    }

    suspend fun getRidesSuspend(): List<DisRide> = suspendCoroutine{ cont ->
        var numUpdates = 0
        appSync.getRides(object: AppSyncTest.GetRidesCallback {
            override fun onResponse(response: List<DisRide>) {
                if (numUpdates == 0) {
                    cont.resume(response)
                }
                numUpdates++
            }

            override fun onError(ec: Int?, msg: String?) {

            }
        }, AppSyncResponseFetchers.NETWORK_FIRST)
    }

    suspend fun getRideUpdatesSuspend(): List<DisRideUpdate>? = suspendCoroutine{ cont ->
        var numUpdates = 0
        appSync.updateRides(object: AppSyncTest.UpdateRidesCallback {
            override fun onResponse(response: List<DisRideUpdate>?) {
                if (numUpdates == 0) {
                    cont.resume(response)
                }
                numUpdates++
            }

            override fun onError(ec: Int?, msg: String?) {

            }
        })
    }

    fun subscribeToRideUpdates(cb: AppSyncTest.RideUpdateSubscribeCallback) {
        appSync.subscribeToRideUpdates(cb)
    }

    fun getRideDPs(rideID: String, cb: AppSyncTest.GetRideDPsCallback) {
        appSync.getRideDPs(rideID, cb)
    }
}