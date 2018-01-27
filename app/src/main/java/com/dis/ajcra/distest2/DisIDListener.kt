package com.dis.ajcra.distest2

import android.content.Intent
import com.google.android.gms.iid.InstanceIDListenerService

/**
 * Created by ajcra on 1/26/2018.
 */
class DisIDListener : InstanceIDListenerService() {
    override fun onTokenRefresh() {
        val intent = Intent(this, TokenIntentService::class.java)
        startService(intent)
    }
}