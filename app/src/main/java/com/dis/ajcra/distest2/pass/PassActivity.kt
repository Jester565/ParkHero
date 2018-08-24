package com.dis.ajcra.distest2.pass

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.R

class PassActivity : FragmentActivity() {
    private lateinit var passFragment: PassFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        passFragment = PassFragment.GetInstance()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.loginactivity_layout, passFragment).commit()
    }
}
