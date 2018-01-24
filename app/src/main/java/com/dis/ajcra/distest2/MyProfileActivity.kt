package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.MyProfileFragment
import com.dis.ajcra.distest2.R

class MyProfileActivity : FragmentActivity() {
    private lateinit var myProfileFragment: MyProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)
        myProfileFragment = MyProfileFragment()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.myprofileactivity_layout, myProfileFragment).commit()
    }
}
