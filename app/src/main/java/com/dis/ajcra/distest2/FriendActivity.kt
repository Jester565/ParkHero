package com.dis.ajcra.distest2

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.login.FriendFragment
import com.dis.ajcra.distest2.login.RegisterFragment

class FriendActivity : FragmentActivity() {
    private lateinit var friendFragment: FriendFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend)
        friendFragment = FriendFragment()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.friendactivity_layout, friendFragment).commit()
    }
}
