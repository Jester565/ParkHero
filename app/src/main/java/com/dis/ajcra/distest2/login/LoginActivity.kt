package com.dis.ajcra.distest2.login

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.R

class LoginActivity : FragmentActivity() {
    private lateinit var loginFragment: LoginFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        var pwd: String? = null
        if (intent.extras != null) {
            if (intent.extras.containsKey("pwd"))
                pwd = intent.extras.getString("pwd")
        }
        loginFragment = LoginFragment.newInstance(pwd)
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.loginactivity_layout, loginFragment).commit()
    }
}
