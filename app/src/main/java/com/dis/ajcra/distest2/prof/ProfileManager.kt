package com.dis.ajcra.distest2.prof

import android.content.Context
import android.util.Log
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import com.dis.ajcra.distest2.DisneyAppClient
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.model.CreateEndpointInput
import com.dis.ajcra.distest2.model.CreateUserInput
import com.dis.ajcra.distest2.model.UserInfo
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Created by ajcra on 1/21/2018.
 */
class ProfileManager {
    var apiClient: DisneyAppClient
    var cognitoManager: CognitoManager

    companion object {
        var PROF_SETTINGS_NAME = "prof_prefs"
    }

    constructor(cognitoManager: CognitoManager) {
        this.cognitoManager = cognitoManager
        val factory = ApiClientFactory()
                .credentialsProvider(cognitoManager.credentialsProvider)
        apiClient = factory.build(DisneyAppClient::class.java)
    }

    fun genMyProfile(): Deferred<MyProfile> = async {
        var profile = getMyProfile().await()
        if (profile == null) {
            profile = createMyProfile().await()
        }
        profile as MyProfile
    }

    fun getMyProfile(): Deferred<MyProfile?> = async {
        var myProfile: MyProfile? = null
        try {
            val output = apiClient.getuserGet(null)
            myProfile = MyProfile(apiClient, output)
        } catch (ex: Exception) {
            Log.d("STATE", "Exception: " + ex.message)
        }
        myProfile
    }

    fun getProfile(id: String): Profile {
        return Profile(apiClient, id)
    }

    fun getProfile(info: UserInfo): Profile {
        return Profile(apiClient, info)
    }

    fun createMyProfile(): Deferred<MyProfile> = async {
        var myProfile = null
        try {
            val input = CreateUserInput()
            input.name = null
            val output = apiClient.createuserPost(input)
            var myProfile = MyProfile(apiClient, cognitoManager.federatedID)
            myProfile.name = output.name
        } catch (ex: Exception) {
            Log.d("STATE", "Create exception: " + ex.message)
        }
        myProfile as MyProfile
    }

    fun getUsers(prefix: String): Deferred<ArrayList<Profile>> = async {
        var profiles = ArrayList<Profile>()
        try {
            val output = apiClient.getusersGet("true", prefix)
            var i = 0
            while (i < output.users.size) {
                var profile = Profile(apiClient, output.users[i])
                profile.inviteStatus = output.inviteStatuses[i]
                profiles.add(profile)
                i++
            }
        } catch (ex: Exception) {
            Log.d("STATE", "Get users exception: " + ex.message)
        }

        profiles
    }
}