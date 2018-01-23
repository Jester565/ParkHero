package com.dis.ajcra.distest2.media

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.dis.ajcra.distest2.R

class ScrollGalleryActivity : FragmentActivity() {
    private lateinit var scrollGalleryFragment: ScrollGalleryFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_gallery)
        scrollGalleryFragment = ScrollGalleryFragment()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.scrollgalleryactivity_layout, scrollGalleryFragment).commit()
    }
}
