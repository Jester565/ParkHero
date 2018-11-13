package com.dis.ajcra.distest2.ride

import android.arch.persistence.room.Room
import android.content.Context
import android.util.Log
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.dis.ajcra.distest2.AppSyncTest
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.fastpass.fragment.DisRide
import com.dis.ajcra.fastpass.fragment.DisRideTime
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.*


class RideManager {
    companion object {
        fun GetInstance(cognitoManager: CognitoManager, ctx: Context): RideManager {
            if (rideManager == null) {
                rideManager = RideManager(cognitoManager, ctx)
            }
            return rideManager!!
        }

        //30 seconds to recall lsit
        private var LIST_TIME_DIF = 30000
        private var rideManager: RideManager? = null
    }

    private var appSync: AppSyncTest
    private var crDb: RideCacheDatabase
    private var rides = ArrayList<CRInfo>()
    private var dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private var lastListTime: Date? = null
    private var reqCount: Int = 0

    constructor(cognitoManager: CognitoManager, ctx: Context) {
        appSync = AppSyncTest.GetInstance(cognitoManager, ctx)
        crDb = Room.databaseBuilder(ctx, RideCacheDatabase::class.java, "rides7").build()
    }

    fun initRide(crInfo: CRInfo, rideID: String, rideInfo: DisRide.Info, rideTime: DisRideTime?) {
        crInfo.id = rideID
        crInfo.name = rideInfo.name()!!
        crInfo.picURL = rideInfo.picUrl()
        crInfo.land = rideInfo.land()
        crInfo.height = rideInfo.height()
        if (rideTime != null) {
            setRideTime(crInfo, rideTime)
        }
    }

    fun setRideTime(crInfo: CRInfo, rideTime: DisRideTime): Boolean {
        var timestamp = dateTimeFormat.parse(rideTime.dateTime()).time
        var previousTimestamp = crInfo.lastChangeTime
        if (previousTimestamp == null || previousTimestamp < timestamp) {
            Log.d("STATE", "RIDE UPDATED")
            crInfo.waitRating = rideTime.waitRating()
            crInfo.waitTime = rideTime.waitTime()
            crInfo.fpTime = rideTime.fastPassTime()
            crInfo.status = rideTime.status()
            crInfo.lastChangeTime = timestamp
            return true
        }
        return false
    }

    fun handleRideUpdate(rideUpdate: DisRide, cb: ListRidesCB): CRInfo? {
        var rideI = rides.binarySearch {
            it.name.compareTo(rideUpdate.info()!!.name()!!)
        }
        if (rideI >= 0) {
            var ride = rides.get(rideI)
            if (setRideTime(ride, rideUpdate.time()!!.fragments()!!.disRideTime())) {
                cb.onUpdate(ride)
                return ride
                //cacheRideTime(ride)
            }
        } else {
            var ride = CRInfo()
            initRide(ride, rideUpdate.id()!!, rideUpdate.info()!!, rideUpdate.time()?.fragments()?.disRideTime())
            var insertIdx = -(rideI + 1)
            rides.add(insertIdx, ride)
            cb.onAdd(ride)
            return ride
            //cacheRide(ride)
        }
        return null
    }

    fun handleUpdatedRideList(rideUpdates: List<DisRide>, cb: ListRidesCB) {
        var updateArr = ArrayList<CRInfo>()
        for (rideUpdate in rideUpdates) {
            var ride = handleRideUpdate(rideUpdate, cb)
            if (ride != null) {
               updateArr.add(ride)
            }
        }
        GlobalScope.async(Dispatchers.IO) {
            for (ride in updateArr) {
                cacheRide(ride)
            }
        }
    }

    interface GetRideCB {
        fun onUpdate(ride: CRInfo)
        fun onFinalUpdate(ride: CRInfo)
    }

    fun getRide(rideID: String, cb: GetRideCB) {
        listRides(object: ListRidesCB {
            override fun init(rides: ArrayList<CRInfo>) {
                for (ride in rides) {
                    if (ride.id == rideID) {
                        cb.onUpdate(ride)
                    }
                }
            }

            override fun onAdd(ride: CRInfo) {
                if (ride.id == rideID) {
                    cb.onUpdate(ride)
                }
            }

            override fun onUpdate(ride: CRInfo) {
                if (ride.id == rideID) {
                    cb.onUpdate(ride)
                }
            }

            override fun onAllUpdated(rides: ArrayList<CRInfo>) {
                for (ride in rides) {
                    if (ride.id == rideID) {
                        cb.onFinalUpdate(ride)
                    }
                }
            }
        })
    }

    interface ListRidesCB {
        fun init(rides: ArrayList<CRInfo>)
        fun onAdd(ride: CRInfo)
        fun onUpdate(ride: CRInfo)
        fun onAllUpdated(rides: ArrayList<CRInfo>)
    }

    fun listRides(cb: ListRidesCB) {
        var now = Date()
        reqCount = 0
        lastListTime = Date()
        _listRides(cb)
        /*
        if (lastListTime == null || now.time - lastListTime!!.time > LIST_TIME_DIF) {

        } else {
            cb.init(rides)
            if (reqCount >= 2) {
                cb.onAllUpdated(rides)
            }
        }
        */
    }

    fun _listRides(cb: ListRidesCB) = GlobalScope.async(Dispatchers.IO) {
        var cachedRidesJob = async(Dispatchers.Main) {
            initRides().await()
            cb.init(rides)
        }

        GlobalScope.async(Dispatchers.Main) {
            appSync.getRides(object: AppSyncTest.GetRidesCallback {
                override fun onResponse(response: List<DisRide>) {
                    GlobalScope.async(Dispatchers.Main) {
                        cachedRidesJob.await()
                        handleUpdatedRideList(response, cb)
                        reqCount++
                        if (reqCount > 1) {
                            cb.onAllUpdated(rides)
                        }
                    }
                }

                override fun onError(ec: Int?, msg: String?) {

                }
            }, AppSyncResponseFetchers.NETWORK_ONLY)
        }

        GlobalScope.async(Dispatchers.Main) {
            appSync.updateRides(object : AppSyncTest.UpdateRidesCallback {
                override fun onResponse(response: List<DisRide>?) {
                    GlobalScope.async(Dispatchers.Main) {
                        cachedRidesJob.await()
                        if (response != null) {
                            handleUpdatedRideList(response, cb)
                        }
                        reqCount++
                        if (reqCount > 1) {
                            cb.onAllUpdated(rides)
                        }
                    }
                }

                override fun onError(ec: Int?, msg: String?) {
                    Log.d("STATE", "UPDATED RIDES ERR: " + msg)
                }
            })
        }
    }

    private fun initRides() = GlobalScope.async(Dispatchers.IO) {
        if (rides.isEmpty()) {
            var cachedRideList = getCachedRides().await()
            rides.addAll(cachedRideList)
        }
    }

    private fun cacheRide(ride: CRInfo) = GlobalScope.async(Dispatchers.IO) {
        crDb.crInfoDao().addCRInfo(ride)
    }

    private fun cacheRideTime(ride: CRInfo) = GlobalScope.async(Dispatchers.IO) {
        crDb.crInfoDao().updateRideTime(ride.id, ride.status, ride.waitTime, ride.fpTime, ride.waitRating, ride.lastChangeTime)
    }

    private fun getCachedRides(): Deferred<List<CRInfo>> = GlobalScope.async(Dispatchers.IO) {
        crDb.crInfoDao().listCacheRides()
    }

    private fun getCachedRides(pinned: Boolean): Deferred<List<CRInfo>> = GlobalScope.async(Dispatchers.IO) {
        crDb.crInfoDao().listCacheRideOfPin(pinned)
    }

    fun getRideDPs(rideID: String, cb: AppSyncTest.GetRideDPsCallback) {
        appSync.getRideDPs(rideID, cb)
    }
}