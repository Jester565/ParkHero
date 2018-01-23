package com.dis.ajcra.distest2.media

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async


class ScrollGalleryPagerAdapter: FragmentStatePagerAdapter {
    constructor(fm: FragmentManager, cfm: CloudFileManager, cognitoManager: CognitoManager)
            :super(fm)
    {
        async(UI) {
            Log.d("STATE", "Running ui")
            var job = async {
                Log.d("STATE", "Running job")
                cfm.listObjects(cognitoManager.federatedID + "/pictures")
            }
            var objs = job.await()
            Log.d("STATE", "Aquired objects")
            if (objs != null) {
                for (obj in objs) {
                    if (obj.key.substring(obj.key.length - 3) == "jpg") {
                        fragments.add(PictureFragment.newInstance(obj.key))
                    } else {
                        fragments.add(VideoFragment.newInstance(obj.key))
                    }
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun getItem(position: Int): Fragment {
        return fragments.get(position)
    }

    override fun getCount(): Int {
        return fragments.size
    }

    private var fragments: ArrayList<Fragment> = ArrayList<Fragment>()
}


class ScrollGalleryFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_scroll_gallery, container, false)
        var cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        var cfm = CloudFileManager(cognitoManager.credentialsProvider, this.context.applicationContext)
        var pagerAdapter = ScrollGalleryPagerAdapter(this.childFragmentManager, cfm, cognitoManager)
        var viewPager: ViewPager = rootView.findViewById(R.id.scrollgallery_pager)
        viewPager.adapter = pagerAdapter
        viewPager.currentItem = 1
        return rootView
    }
}