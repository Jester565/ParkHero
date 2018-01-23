package com.dis.ajcra.distest2.login

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.R

class ResetPwdActivity : FragmentActivity() {
    private lateinit var resetPwdFragment: ResetPwdFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)
        resetPwdFragment = ResetPwdFragment()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.resetpwdactivity_layout, resetPwdFragment).commit()
    }
}
