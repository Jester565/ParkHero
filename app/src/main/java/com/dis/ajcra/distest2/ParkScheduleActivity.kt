package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v4.app.FragmentActivity

class ParkScheduleActivity : FragmentActivity() {
    private lateinit var parkScheduleFragment: ParkScheduleFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_park_schedule)
        parkScheduleFragment = ParkScheduleFragment.GetInstance()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.parkschedule_layout, parkScheduleFragment).commit()
    }
}
