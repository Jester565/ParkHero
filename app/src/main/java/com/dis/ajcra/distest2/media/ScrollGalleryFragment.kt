package com.dis.ajcra.distest2.media

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async




class ScrollGalleryPagerAdapter: FragmentStatePagerAdapter {
    private var activity: Activity
    private var cognitoManager: CognitoManager
    private var cfm: CloudFileManager

    constructor(activity: Activity, fm: FragmentManager, cfm: CloudFileManager, cognitoManager: CognitoManager)
            :super(fm)
    {
        this.activity = activity
        this.cfm = cfm
        this.cognitoManager = cognitoManager
    }

    fun init(targetKey: String): Deferred<Int> = async(UI) {
        Log.d("STATE", "Running ui")
        var job = async {
            Log.d("STATE", "Running job")
            cfm.listObjects("media/" + cognitoManager.federatedID, true)
        }
        var objs = job.await()
        var objKeys = ArrayList<String>()
        var observerPairs = cfm.getObservers(TransferType.UPLOAD)
        for (observerPair in observerPairs) {
            objKeys.add(observerPair.key)
        }
        if (objs != null) {
            objs.forEach { it ->
                if (!objKeys.contains(it.key)) {
                    objKeys.add(it.key)
                }
            }
        }

        var targetKeyI = 0
        for (objKey in objKeys) {
            if (objKey.substring(objKey.length - 3) == "jpg") {
                var picFrag = PictureFragment.newInstance(objKey)
                picFrag.setListener(object: PictureListener {
                    override fun onDelete() {
                        Log.d("STATE", "on delete: " + fragments.size)
                        fragments.remove(picFrag)
                        notifyDataSetChanged()
                        if (count == 0) {
                            activity.finish()
                        }
                    }
                })
                fragments.add(picFrag)
            } else {
                fragments.add(VideoFragment.newInstance(objKey))
            }
            if (targetKey == objKey) {
                targetKeyI = fragments.size - 1
            }
            notifyDataSetChanged()
        }
        targetKeyI
    }

    override fun getItemPosition(`object`: Any): Int {
        // refresh all fragments when data set changed
        return PagerAdapter.POSITION_NONE
    }

    override fun getItem(position: Int): Fragment {
        Log.d("STATE", "Getting item " + position)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_scroll_gallery, container, false)
        var cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        var cfm = CloudFileManager(cognitoManager, this.context!!.applicationContext)
        var pagerAdapter = ScrollGalleryPagerAdapter(activity!!, this.childFragmentManager, cfm, cognitoManager)
        var viewPager: ViewPager = rootView.findViewById(R.id.scrollgallery_pager)
        viewPager.adapter = pagerAdapter
        async(UI) {
            var pagerI = pagerAdapter.init(arguments!!.getString(OBJKEY_PARAM)).await()
            viewPager.currentItem = pagerI
        }
        return rootView
    }

    companion object {
        var OBJKEY_PARAM = "objkey"
        fun GetInstance(objKey: String): ScrollGalleryFragment {
            val fragment = ScrollGalleryFragment()
            val args = Bundle()
            args.putString(OBJKEY_PARAM, objKey)
            fragment.arguments = args
            return fragment
        }
    }
}