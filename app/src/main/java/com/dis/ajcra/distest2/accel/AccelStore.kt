package com.dis.ajcra.distest2.accel

import android.arch.persistence.room.Room
import android.content.Context
import kotlinx.coroutines.*
import java.util.*

class AccelStore {
    /*
    class RideMatch(var rideName: String, var distance: Double, var time: Long) {}
    class MovementMatch(var movementName: String, var time: Long) {}
    */

    companion object {
        fun GetInstance(appCtx: Context): AccelStore {
            if (AStore == null) {
                AStore = AccelStore(appCtx)
            }
            return AStore!!
        }
        private var AStore:AccelStore? = null
    }

    private var accelDB: AccelDatabase
    private var rideMatchSubscriptions = HashMap<UUID, (RideMatch) -> Unit>()
    private var movementMatchSubscriptions = HashMap<UUID, (MovementMatch) -> Unit>()

    constructor(ctx: Context) {
        accelDB = Room.databaseBuilder(ctx, AccelDatabase::class.java, "accel").build()
    }

    fun subscribeToRideMatches(cb: (RideMatch) -> Unit): UUID {
        var id = UUID.randomUUID()
        rideMatchSubscriptions[id] = cb
        return id
    }

    fun subscribeToMovementMatches(cb: (MovementMatch) -> Unit): UUID {
        var id = UUID.randomUUID()
        movementMatchSubscriptions[id] = cb
        return id
    }

    fun unsubscribeFromRideMatches(id: UUID) {
        movementMatchSubscriptions.remove(id)
    }

    fun unsubscribeFromMovementMatches(id: UUID) {
        rideMatchSubscriptions.remove(id)
    }

    fun getRideMatches(): Deferred<List<RideMatch>> = GlobalScope.async(Dispatchers.IO) {
        accelDB.accelDatabaseDao().listRideMatches()
    }

    fun getMovementMatches(): Deferred<List<MovementMatch>> = GlobalScope.async(Dispatchers.IO) {
        accelDB.accelDatabaseDao().listMovementMatches()
    }

    fun storeRideMatch(name: String, distance: Double, time: Long) = GlobalScope.async(Dispatchers.IO) {
        var rm = RideMatch()
        rm.name = name
        rm.distance = distance
        rm.time = time
        accelDB.accelDatabaseDao().insertRideMatch(rm)
        GlobalScope.launch(Dispatchers.Main) {
            rideMatchSubscriptions.forEach { it ->
                it.value.invoke(rm)
            }
        }
    }

    fun storeMovementMatch(name: String, confidence: Int, time: Long) = GlobalScope.async(Dispatchers.IO) {
        var mm = MovementMatch()
        mm.name = name
        mm.confidence = confidence
        mm.time = time
        accelDB.accelDatabaseDao().insertMovementMatch(mm)
        GlobalScope.launch(Dispatchers.Main) {
            movementMatchSubscriptions.forEach { it ->
                it.value.invoke(mm)
            }
        }
    }
}