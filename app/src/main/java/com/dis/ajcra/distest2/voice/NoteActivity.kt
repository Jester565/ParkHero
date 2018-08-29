package com.dis.ajcra.distest2.voice

import android.app.Activity
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.ride.CRInfo
import com.dis.ajcra.distest2.ride.Ride
import com.dis.ajcra.distest2.ride.RideManager
import java.sql.Time
import java.util.*


class NoteActivity: Activity() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var rideManager: RideManager
    private lateinit var textToSpeech: TextToSpeech
    private var dialogue: String = ""
    private var ttsInitialized: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this@NoteActivity.applicationContext)
        rideManager = RideManager(cognitoManager, applicationContext)
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

        Log.d("STATE", "QUERY: " + query)

        rideManager.listRides(object: RideManager.ListRidesCB {
            override fun init(rides: ArrayList<CRInfo>) {}
            override fun onAdd(ride: CRInfo) {}
            override fun onUpdate(ride: CRInfo) {}

            override fun onAllUpdated(rides: ArrayList<CRInfo>) {
                var closestRide: CRInfo? = null
                var maxNameScore: Double = -Double.MAX_VALUE
                for (ride in rides) {
                    var rideName = ride.name
                    var score = StringComp.compareStrings(query, rideName)
                    if (score > maxNameScore) {
                        closestRide = ride
                        maxNameScore = score
                    }
                }
                if (closestRide != null) {
                    if (wtScore <= 1 && fpScore <= 1 || wtScore == fpScore) {
                        Log.d("STATE", "p1")
                        dialogue = closestRide.name
                        var waitTime = closestRide.waitTime
                        var fpTime: Time? = null
                        if (closestRide.fpTime != null) {
                            fpTime = Time.valueOf(closestRide.fpTime)
                        }

                        var status = closestRide.status
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
                        var fpTime = Time.valueOf(closestRide.fpTime)
                        if (fpTime != null) {
                            dialogue = timeToStr(fpTime)
                        } else {
                            dialogue = "Fast passes for " + closestRide.name + " are no longer available"
                        }
                    } else {
                        var waitTime = closestRide.waitTime
                        if (closestRide.status == Ride.OPEN_STATUS && waitTime != null) {
                            dialogue = waitTime.toString()
                        } else {
                            dialogue = closestRide.name + " is currently " + closestRide.status
                        }
                    }
                    speakDialogue()
                }
            }
        })
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