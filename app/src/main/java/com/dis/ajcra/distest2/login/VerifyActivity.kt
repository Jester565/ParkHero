package com.dis.ajcra.distest2.login

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.R

class VerifyActivity : FragmentActivity() {
    private lateinit var verifyFragment: VerifyFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)
        var pwd: String? = null
        var delvMeth: String? = null
        var delvDest: String? = null
        var extras = intent.extras
        if (extras.containsKey("pwd"))
            pwd = extras.getString("pwd")
        if (extras.containsKey("delvMeth"))
            delvMeth = extras.getString("delvMeth")
        if (extras.containsKey("delvDest"))
            delvDest = extras.getString("delvDest")
        verifyFragment = VerifyFragment.newInstance(pwd, delvMeth, delvDest)
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.verifyactivity_layout, verifyFragment).commit()
    }
}
