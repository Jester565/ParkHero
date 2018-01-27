package com.dis.ajcra.distest2

import android.app.IntentService
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log

import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest
import com.amazonaws.services.sns.model.NotFoundException
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.prof.MyProfile
import com.dis.ajcra.distest2.prof.ProfileManager

import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID
import kotlinx.coroutines.experimental.async

import java.util.HashMap

class TokenIntentService : IntentService("RegIntentService") {

    private val token: String?
        get() {
            val instanceID = InstanceID.getInstance(this)
            try {
                return instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null)
            } catch (ex: Exception) {
                Log.d("STATE", "Get token failed " + ex)
            }

            return null
        }

    private lateinit var cognitoManager: CognitoManager
    private lateinit var client: AmazonSNSClient
    private lateinit var profileManager: ProfileManager

    override fun onHandleIntent(intent: Intent?) {
        Log.d("STATE", "Starting intent")
        cognitoManager = CognitoManager.GetInstance(applicationContext)
        profileManager = ProfileManager(cognitoManager)
        client = AmazonSNSClient(cognitoManager!!.credentialsProvider)
        client!!.setRegion(Region.getRegion(Regions.US_WEST_2))

        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        var endpointArn = getEndpointArn(preferences)
        val token = token
        var updatedRequired = false
        if (endpointArn == null) {
            endpointArn = createEndpoint(token)
        }
        Log.d("STATE", "Starting intent2")
        try {
            val req = GetEndpointAttributesRequest()
                    .withEndpointArn(endpointArn)
            val result = client!!.getEndpointAttributes(req)
            updatedRequired = result.attributes["Token"] != token || result.attributes["Enabled"].equals("true", ignoreCase = true)
        } catch (ex: NotFoundException) {
            Log.d("STATE", "GetEndpoint not found, recreating")
            endpointArn = createEndpoint(token)
        } catch (ex: Exception) {
            Log.d("STATE", "GetEndpointAttributes ex: " + ex)
        }

        Log.d("STATE", "Starting intent3")
        if (updatedRequired) {
            val attribs = HashMap<String?, String?>()
            attribs.put("Token", token)
            attribs.put("Enabled", "true")
            val req = SetEndpointAttributesRequest()
                    .withEndpointArn(endpointArn)
                    .withAttributes(attribs)
            client!!.setEndpointAttributes(req)
            async {
                Log.d("STATE", "Sending endpoint arn")
                var myProfile = profileManager.getMyProfile().await() as MyProfile
                myProfile.setEndpointArn(endpointArn as String)
            }
        }
    }

    private fun createEndpoint(token: String?): String? {
        try {
            val req = CreatePlatformEndpointRequest()
                    .withPlatformApplicationArn(PLATFORM_APP_ARN)
                    .withToken(token)
                    .withCustomUserData(cognitoManager!!.federatedID)
            val result = client!!.createPlatformEndpoint(req)
            return result.endpointArn
        } catch (ex: Exception) {
            Log.d("STATE", "CreatePlatformEndpoint err " + ex)
        }

        return null
    }

    private fun getEndpointArn(preferences: SharedPreferences): String? {
        return preferences.getString("snsArn", null)
    }

    private fun saveEndpointArn(preferences: SharedPreferences, arn: String) {
        val editor = preferences.edit()
        editor.putString("snsArn", arn)
        editor.commit()
    }

    companion object {
        private val PLATFORM_APP_ARN = "arn:aws:sns:us-west-2:387396130957:app/GCM/DisneyApp"
    }
}
