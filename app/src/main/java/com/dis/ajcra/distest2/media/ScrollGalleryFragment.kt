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
import kotlinx.coroutines.*

class ScrollGalleryPagerAdapter: FragmentStatePagerAdapter {
    private var activity: Activity
    private var cognitoManager: CognitoManager
    private var cfm: CloudFileManager
    private var fragments: ArrayList<Fragment> = ArrayList()

    constructor(activity: Activity, fm: FragmentManager, cfm: CloudFileManager, cognitoManager: CognitoManager)
            :super(fm)
    {
        this.activity = activity
        this.cfm = cfm
        this.cognitoManager = cognitoManager
    }

    fun init(targetKey: String): Deferred<Int> = GlobalScope.async(Dispatchers.Main) {
        Log.d("STATE", "Running ui")
        var job = GlobalScope.async(Dispatchers.IO) {
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

        fragments.clear()
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
        }
        notifyDataSetChanged()
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
}


class ScrollGalleryFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager

    private lateinit var pagerAdapter: ScrollGalleryPagerAdapter
    private lateinit var viewPager: ViewPager

    private lateinit var subLoginToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager(context!!.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_scroll_gallery, container, false)
        var cfm = CloudFileManager(cognitoManager, this.context!!.applicationContext)
        pagerAdapter = ScrollGalleryPagerAdapter(activity!!, this.childFragmentManager, cfm, cognitoManager)
        viewPager = rootView.findViewById(R.id.scrollgallery_pager)
        viewPager.adapter = pagerAdapter
        return rootView
    }

    override fun onResume() {
        super.onResume()
        subLoginToken = cognitoManager.subscribeToLogin { ex ->
            if (ex == null) {
                GlobalScope.launch(Dispatchers.Main) {
                    var pagerI = pagerAdapter.init(arguments!!.getString(OBJKEY_PARAM)).await()
                    viewPager.currentItem = pagerI
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cognitoManager.unsubscribeFromLogin(subLoginToken)
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