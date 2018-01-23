package com.dis.ajcra.distest2.login

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.R

class RegisterActivity : FragmentActivity() {
    private lateinit var registerFragment: RegisterFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        registerFragment = RegisterFragment()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.registeractivity_layout, registerFragment).commit()
    }
}
