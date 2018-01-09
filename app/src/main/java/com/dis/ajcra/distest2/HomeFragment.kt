package com.dis.ajcra.distest2

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import kotlinx.coroutines.experimental.async

class HomeFragment : Fragment() {
    companion object {
        var BUCKET_NAME = "disneyapp"
    }
    lateinit var cognitoManager: CognitoManager
    lateinit var s3Client: AmazonS3Client
    lateinit var profilePicView: ImageView

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_home, container, false)
        profilePicView = rootView.findViewById(R.id.home_profilepic)

        cognitoManager = CognitoManager.GetInstance(activity.applicationContext)
        s3Client =  AmazonS3Client(cognitoManager.credentialsProvider)

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initProfilePic()
    }

    fun initProfilePic() {
        var objReq: GetObjectRequest = GetObjectRequest(BUCKET_NAME, "profileImgs/blank-profile-picture-973460_640.png")
        async {
            Log.d("STATE", "Getting image")
            try {
                var response = s3Client.getObject(objReq)
                var bmp = BitmapFactory.decodeStream(response.objectContent);
                Log.d("STATE", "Bmp Loaded")
                activity.runOnUiThread {
                    var roundedBmp = RoundedBitmapDrawableFactory.create(resources, bmp)
                    roundedBmp.isCircular = true
                    profilePicView.setImageDrawable(roundedBmp)
                    Log.d("STATE", "Updated profile pic view")
                }
            } catch (e: Exception) {
                Log.d("STATE", "Exception occured")
                Log.d("STATE", e.message)
            }
        }
    }
}
