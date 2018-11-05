package com.dis.ajcra.distest2.ride

import android.util.Log
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import com.amazonaws.regions.RegionUtils.init
import com.dis.ajcra.distest2.model.*
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.fastpass.fragment.DisRide
import com.dis.ajcra.fastpass.fragment.DisRideTime
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.json.JSONObject
import java.sql.Date
import java.sql.Time
import java.text.SimpleDateFormat
import java.time.LocalTime

/**
 * Created by ajcra on 2/13/2018.
 */
class Ride {
    var id: String
    var info: DisRide.Info
    var time: DisRideTime? = null

    companion object {
        val OPEN_STATUS: String = "Operating"
        val DOWN_STATUS: String = "Down"
        val CLOSE_STATUS: String = "Closed"
    }

    constructor(ride: DisRide) {
        this.id = ride.id()!!
        this.info = ride.info()!!
        this.time = ride.time()?.fragments()?.disRideTime()
    }

    fun setRideTime(rideTime: DisRideTime) {
        this.time = rideTime
    }

    /*
    fun getPredictedTimes(): Deferred<List<RideTimeInfo>> = async {
        var arr: List<RideTimeInfo>? = null
        try {
            var result = apiClient.getridedpGet(id.toString())
            arr = result.predictTimes
        } catch (ex: Exception) {
            Log.d("STATE", "EX: " + ex)
        }
        arr as List<RideTimeInfo>
    }

    fun getRideTimes(): Deferred<List<RideTimeInfo>> = async {
        var arr: List<RideTimeInfo>? = null
        try {
            var result = apiClient.getridedpGet(id.toString())
            arr = result.rideTimes
        } catch (ex: Exception) {
            Log.d("STATE", "EX: " + ex)
        }
        arr as List<RideTimeInfo>
    }
    */
}