package com.dis.ajcra.distest2.ride

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.R

class RideActivity : FragmentActivity() {
    private lateinit var rideFragment: RideFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        var id: String = intent.extras.getString("id")
        rideFragment = RideFragment.GetInstance(id)
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.loginactivity_layout, rideFragment).commit()
    }
}
