package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v4.app.*
import android.support.v4.view.ViewPager
import com.google.firebase.iid.FirebaseInstanceId

class DlPagerAdapter: FragmentPagerAdapter {
    constructor(fm: FragmentManager)
            :super(fm)
    {
        fragments.add(CameraFragment())
        fragments.add(HomeFragment())
        fragments.add(RideTimesFragment())
    }

    override fun getItem(position: Int): Fragment {
        return fragments.get(position)
    }

    override fun getCount(): Int {
        return fragments.size
    }

    private var fragments: ArrayList<Fragment> = ArrayList<Fragment>()
}

class MainActivity: FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var pagerAdapter = DlPagerAdapter(supportFragmentManager)
        var viewPager = findViewById<ViewPager>(R.id.main_viewpager)
        viewPager.adapter = pagerAdapter
        viewPager.currentItem = 1
    }
}
