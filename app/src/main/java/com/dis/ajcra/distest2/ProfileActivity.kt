package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.LoginFragment

class ProfileActivity : FragmentActivity() {
    private lateinit var profileFragment: ProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        var id: String = intent.extras.getString("id")
        profileFragment = ProfileFragment.newInstance(id)
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.loginactivity_layout, profileFragment).commit()
    }
}
