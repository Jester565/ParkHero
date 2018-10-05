package com.dis.ajcra.distest2.accel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.*

class MovementBroadcastReceiver(private var firebaseAnalytics: FirebaseAnalytics, private var accelStore: AccelStore): BroadcastReceiver() {
    override fun onReceive(ctx: Context?, intent: Intent?) {
        if (intent != null) {
            var v = "Activity :" + intent.getStringExtra("Activity") + " " + "Confidence : " + intent.extras!!.getInt("Confidence") + "n"
            Log.d("MOVEMENT_MATCH", v)
            kotlin.run {
                var bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "MOVEMENT_MATCH")
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "MOVEMENT_MATCH")
                bundle.putString(FirebaseAnalytics.Param.CONTENT, v)
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text")
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }
            var name = intent.getStringExtra("Activity")
            var confidence = intent.extras!!.getInt("Confidence")
            var time = Date().time
            accelStore?.storeMovementMatch(name, confidence, time)
        }
    }
}