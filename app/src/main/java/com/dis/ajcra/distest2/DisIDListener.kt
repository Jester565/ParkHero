package com.dis.ajcra.distest2

import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.*
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.prof.ProfileManager
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import java.util.*


/**
 * Created by ajcra on 1/26/2018.
 */
class DisIDListener : FirebaseInstanceIdService() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var client: AmazonSNSClient
    private lateinit var profileManager: ProfileManager

    override fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        if (refreshedToken != null) {
            sendRegistrationToSns(refreshedToken)
        }
    }

    fun sendRegistrationToSns(token: String) {
        cognitoManager = CognitoManager.GetInstance(applicationContext)
        profileManager = ProfileManager(cognitoManager, applicationContext)
        client = AmazonSNSClient(cognitoManager!!.credentialsProvider)
        client!!.setRegion(Region.getRegion(Regions.US_WEST_2))

        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        var endpointArn = getEndpointArn(preferences)
        var updateRequired = false
        if (endpointArn == null) {
            endpointArn = createEndpoint(token)
        }
        try {
            val req = GetEndpointAttributesRequest()
                    .withEndpointArn(endpointArn)
            val result = client!!.getEndpointAttributes(req)
            updateRequired = result.attributes["Token"] != token || result.attributes["Enabled"].equals("false", ignoreCase = true)
        } catch (ex: NotFoundException) {
            Log.d("STATE", "GetEndpoint not found, recreating")
            endpointArn = createEndpoint(token)
        } catch (ex: Exception) {
            Log.d("STATE", "GetEndpointAttributes ex: " + ex)
        }

        if (updateRequired) {
            val attribs = HashMap<String?, String?>()
            attribs.put("Token", token)
            attribs.put("Enabled", "true")
            val req = SetEndpointAttributesRequest()
                    .withEndpointArn(endpointArn)
                    .withAttributes(attribs)
            client!!.setEndpointAttributes(req)
        }
    }

    private fun createEndpoint(token: String?): String? {
        var topicName = cognitoManager.federatedID.substring(cognitoManager.federatedID.indexOf(':') + 1)
        var topicArn: String? = null
        try {
            var req = client!!.createTopic(topicName)
            topicArn = req.topicArn
        } catch (ex: Exception) {
            Log.d("STATE", "Could not create topic: " + ex.message)
        }
        try {
            val req = CreatePlatformEndpointRequest()
                    .withPlatformApplicationArn(PLATFORM_APP_ARN)
                    .withToken(token)
                    .withCustomUserData(cognitoManager!!.federatedID)
            val result = client!!.createPlatformEndpoint(req)
            var subReq = SubscribeRequest()
            subReq.endpoint = result.endpointArn
            subReq.protocol = "application"
            subReq.topicArn = topicArn
            client!!.subscribe(subReq)
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