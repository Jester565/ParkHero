package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import com.dis.ajcra.distest2.camera.CameraFragment
import com.dis.ajcra.distest2.ride.RideTimesFragment

class DlPagerAdapter: FragmentPagerAdapter {
    constructor(fm: FragmentManager)
            :super(fm)
    {
        cameraFragment = CameraFragment.GetInstance()
        homeFragment = HomeFragment()
        rideTimesFragment = RideTimesFragment()
        fragments.add(cameraFragment)
        fragments.add(homeFragment)
        fragments.add(rideTimesFragment)
    }

    override fun getItem(position: Int): Fragment {
        return fragments.get(position)
    }

    override fun getCount(): Int {
        return fragments.size
    }

    fun onBackButtonPressed(viewPager: ViewPager): Boolean {
        if (viewPager.currentItem == 2 && !rideTimesFragment.handleBackButton()) {
            return false
        } else if (viewPager.currentItem == 0 && !cameraFragment.handleBackButton()) {
            return false
        } else if (viewPager.currentItem != 1) {
            viewPager.currentItem = 1
            return false
        }
        return true
    }

    private var cameraFragment: CameraFragment
    private var homeFragment: HomeFragment
    private var rideTimesFragment: RideTimesFragment
    private var fragments: ArrayList<Fragment> = ArrayList<Fragment>()
}

class MainActivity: FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pagerAdapter = DlPagerAdapter(supportFragmentManager)
        viewPager = findViewById<ViewPager>(R.id.main_viewpager)
        viewPager!!.adapter = pagerAdapter
        viewPager!!.currentItem = 1
    }

    override fun onBackPressed() {
        if (pagerAdapter == null || pagerAdapter!!.onBackButtonPressed(viewPager!!)) {
            super.onBackPressed()
        }
    }

    private var viewPager: ViewPager? = null
    private var pagerAdapter: DlPagerAdapter? = null
}
