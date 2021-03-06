package com.dis.ajcra.distest2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.*
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.ProfileActivity
import com.dis.ajcra.distest2.prof.ProfileManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.File
import java.util.*


class SnsEvent {
    var type: String
    var payload: JSONObject

    constructor(type: String, payload: JSONObject) {
        this.type = type
        this.payload = payload
    }
}

class DisGcmListener : FirebaseMessagingService() {
    companion object {
        val FRIEND_INVITE = "FriendInvite"
        val FRIEND_ADDED = "FriendAdded"
        val FRIEND_REMOVED = "FriendRemoved"
        val ENTITY_SENT = "EntitySent"
        val PARTY_INVITE = "PartyInvite"
        val INVITE_CHANNEL_ID = "DisInvites"
        val MSG_CHANNEL_ID = "DisMsgs"
        val PLATFORM_APP_ARN = "arn:aws:sns:us-west-2:387396130957:app/GCM/DisneyApp"
    }

    private lateinit var cognitoManager: CognitoManager
    private lateinit var client: AmazonSNSClient
    private lateinit var profileManager: ProfileManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendRegistrationToSns(token)
    }

    fun sendRegistrationToSns(token: String) {
        cognitoManager = CognitoManager.GetInstance(applicationContext)
        profileManager = ProfileManager(cognitoManager, applicationContext)
        client = AmazonSNSClient(cognitoManager!!.credentialsProvider)
        client!!.setRegion(Region.getRegion(Regions.US_WEST_2))

        var preferences = applicationContext.getSharedPreferences("SNS", Context.MODE_PRIVATE)
        var endpointArn = getEndpointArn(preferences)
        var updateRequired = false
        if (endpointArn == null) {
            endpointArn = createEndpoint(token, preferences)
        }
        try {
            val req = GetEndpointAttributesRequest()
                    .withEndpointArn(endpointArn)
            val result = client!!.getEndpointAttributes(req)
            updateRequired = result.attributes["Token"] != token || result.attributes["Enabled"].equals("false", ignoreCase = true)
        } catch (ex: NotFoundException) {
            Log.d("STATE", "GetEndpoint not found, recreating")
            endpointArn = createEndpoint(token, preferences)
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

    private fun createEndpoint(token: String?, preferences: SharedPreferences): String? {
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
            preferences.edit().putString("snsArn", result.endpointArn).apply()

            var subReq = SubscribeRequest()
            subReq.endpoint = result.endpointArn
            subReq.protocol = "application"
            subReq.topicArn = topicArn
            client!!.subscribe(subReq)
            preferences.edit().putString("snsTopic", topicName).apply()

            return result.endpointArn
        } catch (ex: Exception) {
            Log.d("STATE", "CreatePlatformEndpoint err " + ex)
        }

        return null
    }

    private fun getEndpointArn(preferences: SharedPreferences): String? {
        return preferences.getString("snsArn", null)
    }

    private fun setEndpointArn(preferences: SharedPreferences, arn: String) {
        preferences.edit().putString("snsArn", arn).apply()
    }

    fun createNotificationChannel(id: String, name: String, desc: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(id, name, importance)
            mChannel.description = desc
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        var data = remoteMessage.data
        var msg = JSONObject(data!!.get("default"))
        var type = msg.getString("type")
        var payload = msg.getJSONObject("payload")
        EventBus.getDefault().post(SnsEvent(type, payload))

        if (type == FRIEND_INVITE) {
            handleFriendInvite(type, payload)
        } else if (type == FRIEND_REMOVED) {
            handleFriendRemoved(type, payload)
        } else if (type == ENTITY_SENT) {
            handleEntitySent(type, payload)
        }
    }

    private fun handleEntitySent(type: String, payload: JSONObject) {
        var cognitoManager = CognitoManager.GetInstance(this.applicationContext)
        var cfm = CloudFileManager.GetInstance(cognitoManager, this.applicationContext)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        GlobalScope.async(Dispatchers.IO) {
            var entityID = payload.getString("id")
            var url = payload.getString("url")
            var ownerName = payload.getJSONObject("owner").getString("name")
            Log.d("EntitySent", "Download called with url " + url)
            cfm.download(entityID, object : CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {
                    Log.d("EntitySent", "Download error: " + ex)
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    Log.d("EntitySent", "On Progress changed")
                }

                override fun onStateChanged(id: Int, state: TransferState?) {
                }

                override fun onComplete(id: Int, file: File) {
                    createNotificationChannel(MSG_CHANNEL_ID, "Messages", "Messages")
                    var notificationID = (type + entityID).hashCode()
                    var options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(file.absolutePath, options)

                    var imgScale = 1
                    while (options.outWidth / imgScale > 400) {
                        imgScale *= 2
                    }
                    options.inJustDecodeBounds = false
                    options.inSampleSize = imgScale
                    var bmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    //Create notification
                    val notificationBuilder = NotificationCompat.Builder(this@DisGcmListener, MSG_CHANNEL_ID)
                            .setContentTitle("Picture from " + ownerName)
                            .setSmallIcon(R.drawable.ic_image_black_24dp)
                            .setContentText("Tag info here?")
                            .setLargeIcon(bmap)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(notificationID, notificationBuilder.build())
                }
            }, url)
        }
    }

    private fun handleFriendRemoved(type: String, payload: JSONObject) {
        var removerID = payload.getString("removerId")
        var notificationID = FRIEND_INVITE + removerID
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationID.hashCode())
    }

    private fun handleFriendInvite(type: String, user: JSONObject) {
        var cognitoManager = CognitoManager.GetInstance(this.applicationContext)
        var cfm = CloudFileManager.GetInstance(cognitoManager, this.applicationContext)
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("id", user.getString("id"))
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        var profilePicUrl = user.getString("profilePicUrl")
        if (profilePicUrl == "null") {
            profilePicUrl = "profileImgs/blank-profile-picture-973460_640.png"
        }

        GlobalScope.async(Dispatchers.IO) {
            cfm.download(profilePicUrl, object : CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                }

                override fun onStateChanged(id: Int, state: TransferState?) {
                }

                override fun onComplete(id: Int, file: File) {
                    createNotificationChannel(INVITE_CHANNEL_ID, "Invites", "Contains inviations")
                    var notificationID = (type + user.getString("id")).hashCode()
                    var options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(file.absolutePath, options)

                    var imgScale = 1

                    while (options.outWidth / imgScale > 400) {
                        imgScale *= 2
                    }
                    options.inJustDecodeBounds = false
                    options.inSampleSize = imgScale
                    var bmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    //Create notification
                    val notificationBuilder = NotificationCompat.Builder(this@DisGcmListener, INVITE_CHANNEL_ID)
                            .setContentTitle("Friend Invite From " + user.getString("name"))
                            .setSmallIcon(R.drawable.ic_email_black_24dp)
                            .setContentText("Click to respond")
                            .setLargeIcon(bmap)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(notificationID, notificationBuilder.build())
                }
            })
        }
    }
}
