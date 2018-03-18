package com.dis.ajcra.distest2

import android.util.Log
import com.amazonaws.regions.RegionUtils.init
import com.dis.ajcra.distest2.model.*
import com.dis.ajcra.distest2.prof.Profile
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.json.JSONObject
import java.sql.Date
import java.sql.Time
import java.text.SimpleDateFormat
import java.time.LocalTime

/**
 * Created by ajcra on 2/13/2018.
 */
class Ride {
    var initialized: Boolean = false
    var id: Int
    var name: String? = null
    var picUrl: String? = null
    var status: String? = null
    var waitRating: Float? = null
    var waitTime: Int? = null
    var fastPassTime: Time? = null
    var dateTime: Date? = null
    var apiClient: DisneyAppClient

    constructor(apiClient: DisneyAppClient, id: Int) {
        this.apiClient = apiClient
        this.id = id
    }

    constructor(apiClient: DisneyAppClient, rideInfo: RideInfo) {
        this.apiClient = apiClient
        this.id = rideInfo.id
        setRideInfo(rideInfo)
    }

    fun setRideInfo(rideInfo: RideInfo) {
        initialized = true
        var timeFormatter = SimpleDateFormat("HH:mm")
        var dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        this.name = rideInfo.name
        this.picUrl = rideInfo.picUrl
        this.status = rideInfo.status
        this.waitTime = rideInfo.rideTime.waitTime
        if (rideInfo.rideTime.fastPassTime != null) {
            this.fastPassTime = Time(timeFormatter.parse(rideInfo.rideTime.fastPassTime).time)
        }
        if (rideInfo.rideTime.dateTime != null) {
            this.dateTime = Date(dateTimeFormatter.parse(rideInfo.rideTime.dateTime).time)
        }
        if (rideInfo.waitRating != null) {
            this.waitRating = rideInfo.waitRating.toFloat()
        }
    }

    fun downloadRideInfo() {

    }

    fun getID(): Int {
        return id
    }

    fun getName(): Deferred<String> = async {
        if (!initialized) {
            downloadRideInfo()
        }
        name as String
    }

    fun getStatus(): Deferred<String> = async {
        if (!initialized) {
            downloadRideInfo()
        }
        status as String
    }

    fun getWaitRating(): Deferred<Float?> = async {
        if (!initialized) {
            downloadRideInfo()
        }
        waitRating
    }

    fun getWaitTime(): Deferred<Int?> = async {
        if (!initialized) {
            downloadRideInfo()
        }
        waitTime
    }

    fun getFastPassTime(): Deferred<Time?> = async {
        if (!initialized) {
            downloadRideInfo()
        }
        fastPassTime
    }

    fun getDateTime(): Deferred<Date?> = async {
        if (!initialized) {
            downloadRideInfo()
        }
        dateTime
    }

    fun getPicUrl(): Deferred<String?> = async {
        if (!initialized) {
            downloadRideInfo()
        }
        picUrl
    }

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
}