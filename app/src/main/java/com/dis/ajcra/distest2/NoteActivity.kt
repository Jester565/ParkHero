package com.dis.ajcra.distest2

import android.app.Activity
import android.app.SearchManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.dis.ajcra.distest2.login.CognitoManager
import com.google.android.gms.actions.NoteIntents
import com.google.android.gms.actions.SearchIntents
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.w3c.dom.Text
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import info.debatty.java.stringsimilarity.Damerau
import android.R.attr.data
import android.os.Looper
import android.support.v4.app.NotificationCompat.getExtras
import com.dis.ajcra.fastpass.fragment.DisRide
import java.sql.Time
import java.util.concurrent.locks.Lock


class NoteActivity: Activity() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var rideManager: RideManager
    private lateinit var textToSpeech: TextToSpeech
    private var dialogue: String = ""
    private var ttsInitialized: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this@NoteActivity.applicationContext)
        rideManager = RideManager(this.applicationContext)
        initTTS()

        var intent = getIntent()
        val bundle = intent.getExtras()
        if (bundle != null) {
            for (key in bundle!!.keySet()) {
                val value = bundle!!.get(key)
                Log.d("STATE", String.format("%s %s (%s)", key,
                        value!!.toString(), value!!.javaClass.getName()))
            }
        }
        var query = intent.getStringExtra("android.intent.extra.TEXT").toLowerCase()
        query = remove(query, "get").second
        query = remove(query, "for").second
        var fpScore = 0
        var removeResult = remove(query, "fast")
        fpScore += removeResult.first
        query = removeResult.second
        removeResult = remove(query, "pass")
        fpScore += removeResult.first
        query = removeResult.second
        removeResult = remove(query, "time")
        fpScore += removeResult.first
        query = removeResult.second
        removeResult = remove(query, "times")
        fpScore += removeResult.first
        query = removeResult.second

        var wtScore = 0
        removeResult = remove(query, "wait")
        wtScore += removeResult.first
        query = removeResult.second
        removeResult = remove(query, "line")
        wtScore += removeResult.first
        query = removeResult.second
        removeResult = remove(query, "ride")
        wtScore += removeResult.first
        query = removeResult.second
        removeResult = remove(query, "time")
        wtScore += removeResult.first
        query = removeResult.second
        removeResult = remove(query, "times")
        wtScore += removeResult.first
        query = removeResult.second

        Log.d("STATE", "QUERY NAME: " + query)
        async(UI) {
            var getJob = async(UI) {
                rideManager.getRidesSuspend()
            }
            var updateJob = async(UI) {
                rideManager.getRideUpdatesSuspend()
            }
            var rides = getJob.await()
            var closestRide: DisRide? = null
            var maxNameScore: Double = -Double.MAX_VALUE
            if (rides != null) {
                for (ride in rides) {
                    var rideName = ride.info()!!.name()!!
                    var score = StringComp.compareStrings(query, rideName)
                    if (score > maxNameScore) {
                        closestRide = ride
                        maxNameScore = score
                    }
                }
                Log.d("STATE", "RUNNING")
                if (closestRide != null) {
                    Log.d("STATE", "closest ride found")
                    var disRideTime = closestRide.time()!!.fragments().disRideTime()

                    Log.d("STATE", "AWAITING UPDATE")
                    var rideUpdates = updateJob.await()
                    Log.d("STATE", "UPDATE COMPLETE")

                    if (rideUpdates != null) {
                        var rideUpdate = rideUpdates?.find { it ->
                            it.id() == closestRide.id()
                        }
                        if (rideUpdate != null) {
                            disRideTime = rideUpdate.time()!!.fragments().disRideTime()
                        }
                    }

                    if (wtScore <= 1 && fpScore <= 1 || wtScore == fpScore) {
                        Log.d("STATE", "p1")
                        dialogue = closestRide.info()!!.name()!!
                        var waitTime = disRideTime.waitTime()
                        var fpTime: Time? = null
                        if (disRideTime.fastPassTime() != null) {
                            fpTime = Time.valueOf(disRideTime.fastPassTime())
                        }

                        var status = disRideTime.status()
                        if (status == Ride.OPEN_STATUS && waitTime != null) {
                            dialogue += " has a wait time of " + waitTime.toString() + " minutes"
                            if (fpTime != null) {
                                dialogue += " and "
                            }
                        } else {
                            dialogue += " is currently " + status
                            if (fpTime != null) {
                                dialogue += " but "
                            }
                        }
                        if (fpTime != null) {
                            dialogue += "fast passes are available for " + timeToStr(fpTime)
                        }
                    } else if (fpScore > wtScore) {
                        var fpTime = Time.valueOf(disRideTime.fastPassTime())
                        if (fpTime != null) {
                            dialogue = timeToStr(fpTime)
                        } else {
                            dialogue = "Fast passes for " + closestRide.info()!!.name()!! + " are no longer available"
                        }
                    } else {
                        var waitTime = disRideTime.waitTime()!!
                        if (disRideTime.status()!! == Ride.OPEN_STATUS && waitTime != null) {
                            dialogue = waitTime.toString()
                        } else {
                            dialogue = closestRide.info()!!.name()!! + " is currently " + disRideTime.status()!!
                        }
                    }
                    speakDialogue()
                }
            }
        }
    }

    fun timeToStr(time: Time): String {
        var dayHalf = "am"
        var fpTimeStr = time.toString()
        var hour = fpTimeStr.substring(0, 2).toInt()
        if (hour > 12) {
            hour -= 12
            dayHalf = "pm"
        }
        return hour.toString() + fpTimeStr.substring(2, 5) + " " + dayHalf
    }

    fun remove(str: String, term: String): Pair<Int, String> {
        var matchCount = 0
        var result = str
        do {
            var idx = result.indexOf(term)
            if (idx >= 0) {
                matchCount++
                var startIdx = idx
                var endIdx = idx + term.length
                if (idx > 0) {
                    startIdx--
                }
                if (idx + term.length >= str.length && startIdx == idx) {
                    endIdx++
                }
                result = result.removeRange(startIdx, endIdx)
            }
        }
        while (idx > 0)
        return Pair(matchCount, result)
    }

    fun initTTS() {
        textToSpeech = TextToSpeech(this@NoteActivity, { status ->
            if (status == TextToSpeech.SUCCESS) {
                var langResult = textToSpeech.setLanguage(Locale.US)
                if (langResult != TextToSpeech.LANG_NOT_SUPPORTED && langResult != TextToSpeech.LANG_MISSING_DATA) {
                    ttsInitialized = true
                    speakDialogue()
                }
            }
        })
    }

    fun speakDialogue() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.d("STATE", "ON MAIN THREAD")
        } else {
            Log.d("STATE", "Not on main thread")
        }
        if (ttsInitialized && dialogue.length > 0) {
            var bundle = Bundle()
            textToSpeech.speak(dialogue, TextToSpeech.QUEUE_FLUSH, bundle, "gra")
        }
    }
}