package com.dis.ajcra.distest2

import android.util.Log
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.model.CreateUserInput
import com.dis.ajcra.distest2.model.SendEntityInput
import com.dis.ajcra.distest2.model.UserInfo
import com.dis.ajcra.distest2.prof.MyProfile
import com.dis.ajcra.distest2.prof.Profile
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.json.JSONObject


class RideManager {
    var apiClient: DisneyAppClient

    constructor(credentialsProvider: AWSCredentialsProvider) {
        val factory = ApiClientFactory()
                .credentialsProvider(credentialsProvider)
        apiClient = factory.build(DisneyAppClient::class.java)
    }

    fun getRides(): Deferred<List<Ride>> = async {
        var arr = ArrayList<Ride>()
        try {
            var result = apiClient.getridesGet("", "", "")
            for (rideInfo in result) {
                arr.add(Ride(apiClient, rideInfo))
            }
        } catch (ex: Exception) {
            Log.d("STATE", "getRidesEx: " + ex.message)
        }
        arr
    }
}