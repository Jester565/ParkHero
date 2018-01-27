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
import com.google.android.gms.gcm.GcmListenerService
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.json.JSONObject
import java.io.File
import java.lang.Exception

class DisGcmListener : GcmListenerService() {
    override fun onMessageReceived(from: String?, data: Bundle?) {
        val type = data!!.getString("type")
        var payload = JSONObject(data!!.getString("default"))
        if (payload.getString("type") == "FriendInvite") {
            handleFriendInvite(payload)
        }
    }

    private fun handleFriendInvite(payload: JSONObject) {
        var cognitoManager = CognitoManager.GetInstance(this.applicationContext)
        var cfm = CloudFileManager.GetInstance(cognitoManager.credentialsProvider, this.applicationContext)
        var user =payload.getJSONObject("user")
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("id", user.getString("id"))
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        var profilePicUrl = user.getString("profilePicUrl")
        Log.d("STATE", "ProfilePicUrl: " + profilePicUrl)
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
                    Log.d("STATE", "on complete called")
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
                    val notificationBuilder = NotificationCompat.Builder(this@DisGcmListener, "DISTEST4")
                            .setContentTitle("Friend Invite From " + user.getString("name"))
                            .setSmallIcon(R.drawable.ic_email_black_24dp)
                            .setContentText("Click to respond")
                            .setLargeIcon(bmap)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(0, notificationBuilder.build())
                }
            })
        }
    }
}
