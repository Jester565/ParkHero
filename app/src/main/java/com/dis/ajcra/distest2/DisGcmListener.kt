package com.dis.ajcra.distest2

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.util.DisplayMetrics
import android.util.Log
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.experimental.EventLoop
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import android.app.NotificationChannel
import android.os.Build



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
        var FRIEND_INVITE = "FriendInvite"
        var FRIEND_ADDED = "FriendAdded"
        var FRIEND_REMOVED = "FriendRemoved"
        var ENTITY_SENT = "EntitySent"
        var INVITE_CHANNEL_ID = "DisInvites"
        var MSG_CHANNEL_ID = "DisMsgs"
    }

    fun createNotificationChannel(id: String, desc: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "Invites"
            val description = "Receive notifications when people send invites"
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
        async {
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
                    createNotificationChannel(MSG_CHANNEL_ID, "Messages")
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

        async {
            cfm.download(profilePicUrl, object : CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                }

                override fun onStateChanged(id: Int, state: TransferState?) {
                }

                override fun onComplete(id: Int, file: File) {
                    createNotificationChannel(INVITE_CHANNEL_ID, "Invites")
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
