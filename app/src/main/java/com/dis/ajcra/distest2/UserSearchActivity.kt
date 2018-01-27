package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.login.FriendListFragment
import com.dis.ajcra.distest2.login.InviteListFragment
import com.dis.ajcra.distest2.login.UserSearchFragment

class UserSearchActivity : FragmentActivity() {
    private lateinit var searchFragment: UserSearchFragment
    private lateinit var inviteFragment: InviteListFragment
    private lateinit var friendFragment: FriendListFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_search)
        searchFragment = UserSearchFragment()
        inviteFragment = InviteListFragment()
        friendFragment = FriendListFragment.GetInstance()
        var searchTransaction = supportFragmentManager.beginTransaction()
        searchTransaction.replace(R.id.usersearchactivity_searchlayout, searchFragment).commit()
        var inviteTransaction = supportFragmentManager.beginTransaction()
        inviteTransaction.replace(R.id.usersearchactivity_invitelayout, inviteFragment).commit()
        var friendTransaction = supportFragmentManager.beginTransaction()
        friendTransaction.replace(R.id.usersearchactivity_friendlayout, friendFragment).commit()
    }
}
