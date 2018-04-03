package com.dis.ajcra.distest2

import android.content.Intent
import com.google.firebase.iid.FirebaseInstanceIdService

/**
 * Created by ajcra on 1/26/2018.
 */
class DisIDListener : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        val intent = Intent(this, TokenIntentService::class.java)
        startService(intent)
    }
}