package com.dis.ajcra.distest2

import android.app.Activity
import android.app.SearchManager
import android.os.Bundle
import android.util.Log
import com.google.android.gms.actions.SearchIntents

class SearchActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the intent
        var intent = getIntent()
        Log.d("STATE", "ACTION: " + intent.action + " : " + SearchIntents.ACTION_SEARCH + " : " + SearchManager.QUERY)
        if (SearchIntents.ACTION_SEARCH.equals(intent.getAction())) {
            var query = intent.getStringExtra(SearchManager.QUERY)
            Log.d("STATE", "QUERY DIS: " + query)
        }
    }
}