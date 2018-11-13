package com.dis.ajcra.distest2.pass

import android.animation.ArgbEvaluator
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PagerSnapHelper
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.distest2.prof.ProfileManager
import com.dis.ajcra.fastpass.fragment.DisPass
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.File

class PassFragment : Fragment() {
    companion object {
        fun GetInstance(): PassFragment {
            val fragment = PassFragment()
            return fragment
        }
        private var colorArr = arrayListOf(Color.rgb(255, 200, 200), Color.rgb(200, 255, 200), Color.rgb(200, 200, 255), Color.rgb(255, 200, 255))
    }

    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var passManager: PassManager
    private lateinit var cfm: CloudFileManager

    private var position = 0
    private var scrollLevel = 0

    private lateinit var recyclerLayout: RelativeLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var addButton: FloatingActionButton
    private lateinit var deleteButton: FloatingActionButton
    private lateinit var profileImg: CircleImageView

    private lateinit var emptyLayout: RelativeLayout
    private lateinit var addButton2: FloatingActionButton

    private lateinit var adapter: PassRecyclerAdapter
    private var passProfiles = ArrayList<Profile>()
    private var dataset = ArrayList<DisPass>()

    private var onLoadCB: ((ArrayList<DisPass>) -> Boolean)? = null
    private var onAddCB: (() -> Unit)? = null
    private var onRemoveCB: ((DisPass) -> Unit)? = null
    private var passChangeCB: ((DisPass) -> Unit)? = null

    private lateinit var subLoginToken: String

    private var listPassCB = object: PassManager.ListPassesCB {
        override fun passUpdated(userID: String, passes: List<DisPass>) {
            GlobalScope.async(Dispatchers.Main) {
                var profile: Profile?
                var profileI = passProfiles.indexOfFirst { it ->
                    it.id == userID
                }
                if (profileI < 0) {
                    profileI = passProfiles.size
                    profile = profileManager.getProfile(userID)
                } else {
                    profile = passProfiles.get(profileI)
                }
                if (profile != null) {
                    /*
                       This code should prevent duplicate passes
                    */
                    for (pass in passes) {
                        var existingPassI = dataset.indexOfFirst { it ->
                            it.id() == pass.id()
                        }
                        if (existingPassI < 0) {
                            passProfiles.add(profileI, profile)
                            dataset.add(profileI, pass)
                            adapter.notifyItemInserted(profileI)
                            profileI++
                        } else {
                            //Will the pass information ever change?
                            dataset.set(existingPassI, pass)
                            adapter.notifyItemChanged(existingPassI)
                        }
                    }
                }
            }
        }

        override fun passRemoved(passID: String) {
            var i = dataset.indexOfFirst {
                it.id() == passID
            }
            if (i != null) {
                dataset.removeAt(i)
                adapter.notifyItemRemoved(i)
            }
        }

        override fun updateCompleted() {
            GlobalScope.async(Dispatchers.Main) {
                var updated = onLoadCB?.invoke(dataset)
                if (updated != null && updated) {
                    adapter.notifyDataSetChanged()
                }

                progressBar.visibility = View.GONE
                if (dataset.size > 0) {
                    recyclerLayout.visibility = View.VISIBLE
                    emptyLayout.visibility = View.GONE
                    if (passProfiles.size > 0) {
                        setProfile()
                    }
                } else {
                    recyclerLayout.visibility = View.GONE
                    emptyLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        subLoginToken = cognitoManager.subscribeToLogin { ex ->
            if (ex == null) {
                passManager.subscribeToPasses(listPassCB)
                passManager.listPasses()
            }
        }

    }

    override fun onPause() {
        super.onPause()
        passManager.unsubscribeFromPasses(listPassCB)
        cognitoManager.unsubscribeFromLogin(subLoginToken)
    }

    override fun onStop() {
        super.onStop()
        passManager.unsubscribeFromPasses(listPassCB)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(context!!.applicationContext)
        profileManager = ProfileManager(cognitoManager, context!!.applicationContext)
        passManager = PassManager.GetInstance(cognitoManager, context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_pass, container, false)
        recyclerLayout = rootView.findViewById(R.id.pass_recyclerLayout)
        recyclerView = rootView.findViewById(R.id.pass_recyclerview)
        progressBar = rootView.findViewById(R.id.pass_progressbar)
        addButton = rootView.findViewById(R.id.pass_addButton)
        deleteButton = rootView.findViewById(R.id.pass_deleteButton)
        profileImg = rootView.findViewById(R.id.pass_profimg)

        emptyLayout = rootView.findViewById(R.id.pass_emptyLayout)
        addButton2 = rootView.findViewById(R.id.pass_addButton2)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = PassRecyclerAdapter(dataset)
        val helper = PagerSnapHelper()
        helper.attachToRecyclerView(recyclerView)
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                Log.d("STATE", "StateChange: " + newState)
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    position = (recyclerView?.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                    if (position < 0) {
                        position = 0
                    }
                    scrollLevel = 0
                    setProfile()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView!!, dx, dy)
                var c1 = colorArr.get(position % colorArr.size)
                var c2 = 0
                if (scrollLevel > 0) {
                    c2 = colorArr.get((position + 1) % colorArr.size)
                }  else {
                    if (position - 1 < 0) {
                        c2 = colorArr.get(position % colorArr.size)
                    } else {
                        c2 = colorArr.get((position - 1) % colorArr.size)
                    }
                }
                scrollLevel += dx
                Log.d("STATE", "SCROLL LEVEL: " + scrollLevel)
                recyclerView?.setBackgroundColor(ArgbEvaluator().evaluate(Math.abs(scrollLevel.toFloat() / recyclerView?.width), c1, c2) as Int)
            }
        })

        addButton.setOnClickListener {
            onAddCB?.invoke()
        }
        addButton2.setOnClickListener {
            onAddCB?.invoke()
        }
        deleteButton.setOnClickListener {
            onRemoveCB?.invoke(dataset[position])
        }
        return rootView
    }

    fun setOnLoadCallback(cb: ((ArrayList<DisPass>) -> Boolean)?) {
        this.onLoadCB = cb
    }

    fun setOnAddCallback(cb: (() -> Unit)?) {
        this.onAddCB = cb
    }

    fun setOnRemoveCallback(cb: ((DisPass) -> Unit)?) {
        this.onRemoveCB = cb
    }

    fun setPassChangeCallback(cb: ((DisPass) -> Unit)?) {
        this.passChangeCB = cb
    }

    private fun setProfile() {
        recyclerView.setBackgroundColor(colorArr.get(position % colorArr.size))
        var profile = passProfiles.get(position)
        passChangeCB?.invoke(dataset[position])
        GlobalScope.async(Dispatchers.IO) {
            if (profile != null) {
                Log.d("STATE", "PROFILE IS NULL")
            }
            var profPicUrl = profile.getProfilePicUrl().await()
            if (profPicUrl == null) {
                profPicUrl = "profileImgs/blank-profile-picture-973460_640.png"
            }
            cfm.download(profPicUrl, object : CloudFileListener() {
                override fun onComplete(id: Int, file: File) {
                    async(Dispatchers.IO) {
                        var options = BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeFile(file.absolutePath, options)
                        var imgScale = 1
                        while (options.outWidth / imgScale > 400) {
                            imgScale *= 2
                        }
                        options.inJustDecodeBounds = false
                        options.inSampleSize = imgScale
                        var bmap = BitmapFactory.decodeFile(file.absolutePath, options)
                        GlobalScope.async(Dispatchers.Main) {
                            profileImg.setImageBitmap(bmap)
                        }
                    }
                }
            })
        }
    }
}