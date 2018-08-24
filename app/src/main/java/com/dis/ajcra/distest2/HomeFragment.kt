package com.dis.ajcra.distest2

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.CardView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.dis.ajcra.distest2.accel.SensorActivity
import com.dis.ajcra.distest2.entity.EntityListFragment
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.login.LoginActivity
import com.dis.ajcra.distest2.login.RegisterActivity
import com.dis.ajcra.distest2.prof.MyProfile
import com.dis.ajcra.distest2.prof.ProfileManager
import com.dis.ajcra.distest2.prof.UserSearchActivity
import com.dis.ajcra.fastpass.RidesUpdatedSubscription
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async


class HomeFragment : Fragment() {
    companion object {
        var BUCKET_NAME = "disneyapp"
    }
    lateinit var cognitoManager: CognitoManager
    lateinit var profileManager: ProfileManager
    lateinit var s3Client: AmazonS3Client
    lateinit var profilePicView: ImageView
    lateinit var profileNameText: TextView
    lateinit var signInButton: Button
    lateinit var signUpButton: Button
    lateinit var accountLayout: View
    lateinit var friendButton: ImageButton
    lateinit var myProfileButton: ImageButton
    lateinit var scrollView: NestedScrollView
    lateinit var profView: CardView
    var ridesUpdatedSubscription: AppSyncSubscriptionCall<RidesUpdatedSubscription.Data>? = null
    var profViewH: Int = 0
    var myProfile: MyProfile? = null

    fun launchTestActivity() {
        var intent = Intent(context, ParkScheduleActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home, container, false)
        profilePicView = rootView.findViewById(R.id.home_profilepic)
        signInButton = rootView.findViewById(R.id.home_signin)
        signUpButton = rootView.findViewById(R.id.home_signup)
        friendButton = rootView.findViewById(R.id.home_friendbutton)
        myProfileButton = rootView.findViewById(R.id.home_myprofileButton)
        profileNameText = rootView.findViewById(R.id.home_name)
        accountLayout = rootView.findViewById(R.id.home_accountlayout)
        cognitoManager = CognitoManager.GetInstance(activity!!.applicationContext)
        scrollView = rootView.findViewById(R.id.home_scrollview)
        profView = rootView.findViewById(R.id.home_profilelayout)
        profileManager = ProfileManager(cognitoManager)

        scrollView.setOnScrollChangeListener(object: NestedScrollView.OnScrollChangeListener {
            override fun onScrollChange(v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                if (profViewH == 0) {
                    profViewH = profView.height
                }
                val params = profView.getLayoutParams()
                params.height += oldScrollY - scrollY
                if (params.height < 100) {
                    params.height = 100
                } else if (params.height > profViewH) {
                    params.height = profViewH
                }
                profView.setLayoutParams(params)
            }
        })

        s3Client =  AmazonS3Client(cognitoManager.credentialsProvider)
        async(UI) {
            myProfile = profileManager.genMyProfile().await()
            profileNameText.text = myProfile!!.getName().await()
            if (!cognitoManager.isLoggedIn().await()) {
                accountLayout.visibility = View.VISIBLE
                signInButton.setOnClickListener {
                    var intent = Intent(this@HomeFragment.context, LoginActivity::class.java)
                    startActivity(intent)
                }
                signUpButton.setOnClickListener {
                    var intent = Intent(this@HomeFragment.context, RegisterActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        friendButton.setOnClickListener {
            var intent = Intent(this@HomeFragment.context, UserSearchActivity::class.java)
            startActivity(intent)
        }

        myProfileButton.setOnClickListener {
            var intent = Intent(this@HomeFragment.context, SensorActivity::class.java)
            startActivity(intent)
        }
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initProfilePic()
        initFragments()

        //TEMP
        launchTestActivity()
    }

    fun initFragments() {
        var entityListFragment = EntityListFragment()
        this.childFragmentManager.beginTransaction().replace(R.id.home_entitylistholder, entityListFragment).commit()
    }

    fun initProfilePic() {
        var objReq: GetObjectRequest = GetObjectRequest(BUCKET_NAME, "profileImgs/blank-profile-picture-973460_640.png")
        async {
            try {
                var response = s3Client.getObject(objReq)
                var bmp = BitmapFactory.decodeStream(response.objectContent);
                activity!!.runOnUiThread {
                    var roundedBmp = RoundedBitmapDrawableFactory.create(resources, bmp)
                    roundedBmp.isCircular = true
                    profilePicView.setImageDrawable(roundedBmp)
                    Log.d("STATE", "Updated profile pic view")
                }
            } catch (e: Exception) {
                Log.d("STATE", e.message)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (ridesUpdatedSubscription != null) {
            Log.d("STATE", "CANCELLING SUBSCRIPTION: " + ridesUpdatedSubscription?.isCanceled)
            ridesUpdatedSubscription?.cancel()
        }
    }
}
