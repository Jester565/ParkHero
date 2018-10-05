package com.dis.ajcra.distest2.accel

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.opengl.Matrix
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.accel.AccelService2.Companion.ACCEL_MULT
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import tutorial.Acceleration
import java.io.File
import java.io.FileInputStream
import java.lang.Math.pow
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.sqrt




class RideRecService : Service() {
    companion object {
        const val REC_CHANNEL_ID = "RecAccel"
        const val ACCEL_RATE = 30L
        const val CHECK_RATE = 1000L * 60L * 1L
        const val SMART_WINDOW_SIZE = 800L
        const val SMART_SIZE_CHANGE_LIMIT = 3
        const val SMART_ACCEL_MIN = 10000
        const val MAXIMUM_DISTRIBUTION = 0.75
        const val NUM_SECTIONS = 8
        const val MIN_NONZERO_COUNT = 10
        const val MAX_DISTANCE = 5.5
        const val MOVEMENT_ACTION = "com.dis.ajcra.ditest2.MOVEMENT_MATCH"
    }

    //Data structures for calculations the netAcceleration
    private var r = FloatArray(16, {0.0f})
    private var rinv = FloatArray(16, {0.0f})
    private var trueAcceleration = FloatArray(4, {0.0f})
    private var gravity: FloatArray? = null
    private var geoMagnetic: FloatArray? = null
    private var linearAcceleration = FloatArray(4, {0.0f})
    private var netAcceleration = FloatArray(3, {0.0f})
    private var netAccelCount: Int = 0
    private var testStr: String? = "bigthunder"
    private var testArrs = arrayOf(ArrayList<Int>(), ArrayList<Int>(), ArrayList<Int>())
    private var testI = 0

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var sensorManager: SensorManager
    private lateinit var gravitySensor: Sensor
    private lateinit var geoMagneticSensor: Sensor
    private lateinit var linearAccelerationSensor: Sensor

    private lateinit var cognitoManager: CognitoManager

    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private var movementReceiver: BroadcastReceiver? = null
    private var movementIntent: PendingIntent? = null

    private var running = false

    private var activeMillis = ArrayList<Long>()
    private var activeAccels = arrayOf(ArrayList<Int>(), ArrayList<Int>(), ArrayList<Int>())

    private var millis = ArrayList<Long>()
    private var accels = arrayOf(ArrayList<Int>(), ArrayList<Int>(), ArrayList<Int>())

    private var sectionCounts = ArrayList<Int>()

    private var ridePacks: Acceleration.RidePacks? = null

    private var accelStore: AccelStore? = null

    private var firstCheck = false

    private fun accelToInt(accel: Float): Int {
        return (accel * ACCEL_MULT).toInt()
    }

    private var sensorListener = object: SensorEventListener2 {
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) { }

        override fun onFlushCompleted(p0: Sensor?) { }

        override fun onSensorChanged(evt: SensorEvent?) {
            if (evt != null) {
                when (evt.sensor.type) {
                    Sensor.TYPE_GRAVITY -> {
                        gravity = evt.values
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geoMagnetic = evt.values
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        linearAcceleration[0] = evt.values[0]
                        linearAcceleration[1] = evt.values[1]
                        linearAcceleration[2] = evt.values[2]
                        linearAcceleration[3] = 0f
                    }
                }
                try {
                    if (gravity != null && geoMagnetic != null && linearAcceleration != null) {
                        SensorManager.getRotationMatrix(r, null, gravity, geoMagnetic);
                        Matrix.invertM(rinv, 0, r, 0)
                        Matrix.multiplyMV(trueAcceleration, 0, rinv, 0, linearAcceleration, 0)
                        synchronized(netAcceleration) {
                            netAccelCount++
                            var i = 0
                            while (i < 3) {
                                netAcceleration[i] = netAcceleration[i] * (netAccelCount - 1) / (netAccelCount) + trueAcceleration[i] / netAccelCount
                                i++
                            }
                        }
                    }
                } catch(ex: Exception) {
                    Log.e("STATE", "EX: " + ex.message)
                }
            }
        }
    }

    private fun loadRidePacks() {
        val res = resources
        val in_s = res.openRawResource(R.raw.packs)

        val b = ByteArray(in_s.available())
        in_s.read(b)
        ridePacks = Acceleration.RidePacks.parseFrom(b)
    }

    private fun resetAccels() {
        millis.clear()
        for (arr in accels) {
            arr.clear()
        }
        sectionCounts.clear()

        var size = (CHECK_RATE * NUM_SECTIONS) / ACCEL_RATE
        for (i in (size - 1) downTo 0) {
            millis.add(Date().time - i * ACCEL_RATE)
            accels.forEach { arr->
                arr.add(0)
            }
        }
        for (i in 1..NUM_SECTIONS) {
            sectionCounts.add(size.toInt() / 8)
        }

        //Clear accelerations already added, out of order times would mess up smartAverage
        synchronized(activeAccels) {
            activeMillis.clear()
            activeAccels.forEach { arr->
                arr.clear()
            }
        }
    }

    private fun startCollection() {
        Log.d("ACCEL", "Starting collection")

        accelStore = AccelStore.GetInstance(applicationContext)

        firstCheck = true

        loadRidePacks()
        resetAccels()

        sensorManager.registerListener(sensorListener, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(sensorListener, geoMagneticSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(sensorListener, linearAccelerationSensor, SensorManager.SENSOR_DELAY_FASTEST)

        running = true

        if (testStr != null) {
            //Adds to accelerations every 30 ms
            fixedRateTimer(period = ACCEL_RATE, action = {
                if (!running) {
                    cancel()
                }

                synchronized(netAcceleration) {
                    netAccelCount = 0
                    activeMillis.add(Date().time)
                    if (testI < testArrs[0].size) {
                        activeAccels.forEachIndexed { i, arr ->
                            Log.d("RM", "TA: " + testArrs[i][testI])
                            arr.add(testArrs[i][testI])
                        }
                        testI++
                    } else {
                        activeAccels.forEachIndexed { i, arr ->
                            arr.add(0)
                        }
                    }
                }
            })
        } else {
            //Adds to accelerations every 30 ms
            fixedRateTimer(period = ACCEL_RATE, action = {
                if (!running) {
                    cancel()
                }

                synchronized(netAcceleration) {
                    netAccelCount = 0
                    activeMillis.add(Date().time)
                    activeAccels.forEachIndexed { i, arr ->
                        arr.add(accelToInt(netAcceleration[i]))
                    }
                }
            })
        }

        //Performs checks
        fixedRateTimer(period=CHECK_RATE, action={
            if (!running) {
                cancel()
            }
            if (firstCheck) {
                firstCheck = false
                //accelStore!!.storeRideMatch("TEST", 0.0, Date().time)
                //accelStore!!.storeMovementMatch("TEST", 2, Date().time)
                return@fixedRateTimer
            }
            //Prevent one from modifying and one from reading
            synchronized(netAcceleration) {
                //Remove between indices
                Log.d("STATE", "SIZZE: " + millis.size)
                Log.d("STATE", "COUNT: " + sectionCounts[0])
                millis.subList(0, sectionCounts[0]).clear()
                accels.forEachIndexed { i, arr ->
                    arr.subList(0, sectionCounts[0]).clear()
                }
                sectionCounts.removeAt(0)
                millis.addAll(activeMillis)
                sectionCounts.add(activeMillis.size)
                activeMillis.clear()
                accels.forEachIndexed { i, arr ->
                    arr.addAll(activeAccels[i])
                    activeAccels[i].clear()
                }
            }
            var smartAvgs = smartAverage(millis, accels, SMART_WINDOW_SIZE, SMART_SIZE_CHANGE_LIMIT, SMART_ACCEL_MIN)
            var nonZeroCount = getNonZeroCount(smartAvgs)
            kotlin.run {
                var bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "RIDE_MATCH")
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "NONZERO")
                bundle.putString(FirebaseAnalytics.Param.CONTENT, nonZeroCount.toString())
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text")
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }
            Log.d("RIDE_MATCH", "NONZERO: " + nonZeroCount)
            if (nonZeroCount >= MIN_NONZERO_COUNT) {
                var dist = getDistribution(smartAvgs)
                var updateIsZeros = isZeros(smartAvgs, ((NUM_SECTIONS - 1) * smartAvgs[0].size) / NUM_SECTIONS, smartAvgs[0].size - 1)
                kotlin.run {
                    var bundle = Bundle()
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "RIDE_MATCH")
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Distribution")
                    bundle.putString(FirebaseAnalytics.Param.CONTENT, dist.toString())
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text")
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
                }
                kotlin.run {
                    var bundle = Bundle()
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "RIDE_MATCH")
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Zeros")
                    bundle.putString(FirebaseAnalytics.Param.CONTENT, updateIsZeros.toString())
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text")
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
                }
                Log.d("RIDE_MATCH", "DIST: " + dist + "  UpdateIsZeros: " + updateIsZeros)
                if (ridePacks != null && (dist <= MAXIMUM_DISTRIBUTION || updateIsZeros)) {
                    var result = matchPack(smartAvgs, ridePacks!!, MAX_DISTANCE)
                    Log.d("RIDE_MATCH", result!!.name + result!!.distance)
                    kotlin.run {
                        var bundle = Bundle()
                        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "RIDE_MATCH")
                        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "RIDE_MATCH")
                        if (result == null) {
                            bundle.putString(FirebaseAnalytics.Param.CONTENT, "NULL")
                        } else {
                            bundle.putString(FirebaseAnalytics.Param.CONTENT, result.name + ":" + result.distance)
                        }
                        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text")
                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
                    }
                    if (result != null) {
                        //Convert index back to time (may be slightly off right now)
                        var matchTime = millis.last() - (((smartAvgs.size - result.idx).toDouble()/smartAvgs.size.toDouble()) * NUM_SECTIONS * CHECK_RATE).toLong()
                        accelStore!!.storeRideMatch(result.name, result.distance, matchTime)
                        resetAccels()
                    }
                }
            }
        })
    }



    private fun startMovementMatching() {
        activityRecognitionClient = ActivityRecognition.getClient(baseContext)
        val intent = Intent(MOVEMENT_ACTION)
        movementIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        var task = activityRecognitionClient.requestActivityUpdates(20000, movementIntent)
        task.addOnSuccessListener {
            Log.d("MOVEMENT_MATCH", "ARC SUCCESS")
        }.addOnFailureListener { ex ->
            Log.d("MOVEMENT_MATCH", "ARC FAIL: " + ex.message)
        }

        movementReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                var result = ActivityRecognitionResult.extractResult(intent)
                kotlin.run {
                    var bundle = Bundle()
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "MOVEMENT_MATCH")
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "MOVEMENT_MATCH")
                    bundle.putString(FirebaseAnalytics.Param.CONTENT, result.mostProbableActivity.toString())
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text")
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
                }
                accelStore?.storeMovementMatch(result.mostProbableActivity.toString(), result.mostProbableActivity.confidence, result.time)
            }
        }

        var intentFilter = IntentFilter()
        intentFilter.addAction(MOVEMENT_ACTION)

        registerReceiver(movementReceiver, intentFilter)
    }



    private fun getNonZeroCount(accelArrs: ArrayList<ArrayList<Int>>): Int {
        var count= 0
        accelArrs.forEach { arr->
            arr.forEach { a ->
                if (a != 0) {
                    count++
                }
            }
        }
        return count
    }

    private fun isZeros(accelArrs: ArrayList<ArrayList<Int>>, startI: Int, endI: Int): Boolean {
        Log.d("RIDE_MATCH", "STARTI: " + startI + "  EndI: " + endI + "  Size: " + accelArrs.size)
        accelArrs.forEach { arr ->
            for (i in startI..endI) {
                if (arr[i] != 0) {
                    return false
                }
            }
        }
        return true
    }

    private fun getDistribution(accelArrs: ArrayList<ArrayList<Int>>): Double {
        var idxSum = 0
        var idxCount = 0
        accelArrs.forEach { arr->
            arr.forEachIndexed { idx, a ->
                if (a != 0) {
                    idxSum += idx
                    idxCount++
                }
            }
        }
        return idxSum.toDouble() / (idxCount * accelArrs[0].size).toDouble()
    }

    //Removes noise from acceleration using moving average and sign changes
    private fun smartAverage(millisArr: ArrayList<Long>,
                     accelArrs: Array<ArrayList<Int>>,
                     millisWinowSize: Long,
                     signChangeLimit: Int,
                     limit: Int): ArrayList<ArrayList<Int>> {
        //The window used to calculate moving average
        var windowAccel = ArrayList<Int>()
        //Holds number of acceleration sign changes for the window
        var signChangeWindow = ArrayList<Int>()
        //Resulting smartAvg
        var resultArrs = ArrayList<ArrayList<Int>>()
        accelArrs.forEachIndexed { arrIdx, arr ->
            windowAccel.add(0)
            signChangeWindow.add(0)
            resultArrs.add(ArrayList<Int>())
        }
        var startI = 0
        var endI = 0
        var currentMillis = millisArr.get(0)
        while (true) {
            currentMillis += millisWinowSize / 2
            while (millisArr[startI] < currentMillis - millisWinowSize / 2) {
                accelArrs.forEachIndexed { arrIdx, arr ->
                    windowAccel[arrIdx] -= arr[startI]
                    if (startI > 0 && arr[startI] * arr[startI - 1] < 0) {
                        signChangeWindow[arrIdx] -= 1
                    }
                }
                startI++
            }
            while (millisArr[endI] < currentMillis + millisWinowSize / 2) {
                accelArrs.forEachIndexed { arrIdx, arr ->
                    windowAccel[arrIdx] += arr[endI]
                    if (endI > 0 && arr[endI] * arr[endI - 1] < 0) {
                        signChangeWindow[arrIdx] += 1
                    }
                }
                endI++
                if (endI >= millisArr.size) {
                    return resultArrs
                }
            }
            windowAccel.forEachIndexed { arrIdx, a ->
                var a2 = a/(endI - startI + 1)
                if (signChangeWindow[arrIdx] <= signChangeLimit && kotlin.math.abs(a2) >= limit) {
                    resultArrs[arrIdx].add(a2 / kotlin.math.abs(a2))
                } else {
                    resultArrs[arrIdx].add(0)
                }
            }
        }
    }

    class PackTracker {
        var counts = ArrayList<Int>()
        var idxSum = ArrayList<Int>()
        var lastStartAccel = ArrayList<Int>()
        var lastEndAccel = ArrayList<Int>()
    }

    class MatchResult(var idx: Int,
                      var name: String,
                      var distance: Double) {}

    private fun matchPack(accelArrs: ArrayList<ArrayList<Int>>, ridePacks: Acceleration.RidePacks, distanceLimit: Double): MatchResult? {
        var packTrackers = ArrayList<RideRecService.PackTracker>()
        //Initialize packTrackers
        ridePacks.packsList.forEachIndexed { packIdx, pack ->
            packTrackers.add(PackTracker())
            accelArrs.forEachIndexed { arrIdx, arr ->
                packTrackers[packIdx].counts.add(0)
                packTrackers[packIdx].idxSum.add(0)
                packTrackers[packIdx].lastStartAccel.add(0)
                packTrackers[packIdx].lastEndAccel.add(0)
            }
        }
        //Store information of the closest ride pack
        var closestPackName: String? = null
        var closestPackIdx: Int? = null
        var closestPackDistance = distanceLimit
        //Go through all acceleration points
        accelArrs[0].forEachIndexed { idx, _ ->
            packTrackers.forEachIndexed { packIdx, packTracker ->
                var pack = ridePacks.getPacks(packIdx)
                //We're far enough into accelerations so the size matches the ride duration
                if (idx > pack.duration) {
                    var countDistNum = 0  //numerator for count component of distance
                    var countDistDenom = 0 //denominator for count component of distance
                    var totalCounts = 0    //total number of counts for all acceleration components (kind of doubled right now)
                    accelArrs.forEachIndexed { arrIdx, arr ->
                        countDistNum += pow((packTracker.counts[arrIdx] - pack.getCounts(arrIdx)).toDouble(), 2.0).toInt()
                        countDistDenom += pow((pack.getDistributions(arrIdx)).toDouble(), 2.0).toInt()
                        totalCounts += pack.getCounts(arrIdx) + packTracker.counts[arrIdx]  //may not want to add both counts
                    }
                    //The distance used to calculate similarity
                    var dist = sqrt(countDistNum.toDouble()) / sqrt(countDistDenom.toDouble())
                    //Measure difference in distribution, the weight of each distribution is based on how many counts the component has
                    accelArrs.forEachIndexed { arrIdx, arr ->
                        dist += ((kotlin.math.abs(packTracker.idxSum[arrIdx] - pack.getDistributions(arrIdx).toInt() - packTracker.counts[arrIdx] * (idx - pack.duration)).toDouble()
                                / pack.duration.toDouble()) * (packTracker.counts[arrIdx] + pack.getCounts(arrIdx)).toDouble() / totalCounts.toDouble()) / 3.0
                    }

                    //TODO: Favor packs with larger counts

                    //Set dist if better
                    if (dist < closestPackDistance) {
                        closestPackDistance = dist
                        closestPackIdx = idx - pack.duration
                        closestPackName = pack.name
                    }
                    //Shift window by removing counts and idxSum from packTracker
                    accelArrs.forEachIndexed { arrIdx, arr ->
                        var a = accelArrs[arrIdx][idx - pack.duration]
                        if (a != packTracker.lastStartAccel[arrIdx]) {
                            packTracker.lastStartAccel[arrIdx] = a
                            packTracker.counts[arrIdx]--
                            packTracker.idxSum[arrIdx] -= (idx - pack.duration)
                        }
                    }
                }
                //Add current point to packTracker
                accelArrs.forEachIndexed { arrIdx, arr ->
                    var a = accelArrs[arrIdx][idx]
                    if (a != packTracker.lastEndAccel[arrIdx]) {
                        packTracker.lastEndAccel[arrIdx] = a
                        packTracker.counts[arrIdx]++
                        packTracker.idxSum[arrIdx] += idx
                    }
                }
            }
        }
        if (closestPackName != null) {
            return MatchResult(closestPackIdx!!, closestPackName!!, closestPackDistance)
        }
        return null
    }

    private fun stopCollection() {
        Log.d("ACCEL", "Stopping recognition")
        running = false
        firstCheck = false
        sensorManager.unregisterListener(sensorListener)
        movementIntent?.cancel()
        unregisterReceiver(movementReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        createNotificationChannel(REC_CHANNEL_ID, "Recognition", "Recongizes Rides")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notification = Notification.Builder(applicationContext, REC_CHANNEL_ID)
                    .setContentTitle("Recognition")
                    .setContentText("Monitoring Acceleration")
                    .build()
            startForeground(101, notification)
        } else {
            val notification = NotificationCompat.Builder(this)
                    .setContentTitle("ACCEL")
                    .setContentText("Monitoring Acceleration")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

            startForeground(101, notification)
        }

        cognitoManager = CognitoManager.GetInstance(applicationContext)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        geoMagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (testStr != null) {
            async(UI) {
                var cfm = CloudFileManager.GetInstance(CognitoManager.GetInstance(applicationContext), applicationContext)
                Log.d("RIDE_MATCH", "Downloading test file")
                cfm.download("rideAccels/" + testStr, object : CloudFileListener() {
                    override fun onError(id: Int, ex: Exception?) {
                        super.onError(id, ex)
                        Log.d("RIDE_MATCH", "ERROR: " + ex?.message)
                    }
                    override fun onComplete(id: Int, file: File) {
                        super.onComplete(id, file)
                        Log.d("RIDE_MATCH", "Downloaded")
                        var accelPack = Acceleration.AccelerationData.parseFrom(FileInputStream(file))
                        accelPack.xList.forEachIndexed { i, x ->
                            testArrs[0].add(accelPack.xList[i])
                            testArrs[1].add(accelPack.yList[i])
                            testArrs[2].add(accelPack.zList[i])
                        }
                        startCollection()
                    }
                })
            }
        } else {
            startCollection()
        }
        startMovementMatching()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCollection()
    }

    private fun createNotificationChannel(id: String, name: String, desc: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(id, name, importance)
            mChannel.description = desc
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }
}
