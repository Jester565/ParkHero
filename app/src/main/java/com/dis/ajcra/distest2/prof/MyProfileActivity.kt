package com.dis.ajcra.distest2.prof

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.KeyEvent
import com.dis.ajcra.distest2.R



class MyProfileActivity : FragmentActivity() {
    private var myProfileFragment: MyProfileFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)
        myProfileFragment = MyProfileFragment()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.myprofileactivity_layout, myProfileFragment!!).commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var fragmentResult = myProfileFragment?.onKeyDown(keyCode, event)
        if (fragmentResult != null && fragmentResult) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
