package com.dis.ajcra.distest2

import android.app.Activity
import android.app.SearchManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.google.android.gms.actions.SearchIntents

class PlayActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the intent
        var intent = getIntent()
        Log.d("STATE", "ACTION: " + intent.action + " : " + SearchIntents.ACTION_SEARCH + " : " + SearchManager.QUERY)
        if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.action)) {
            var query = intent.getStringExtra(SearchManager.QUERY)
            Log.d("STATE", "QUERY DIS: " + query)
        }
    }
}