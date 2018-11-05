package com.dis.ajcra.distest2.prof

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File


/**
 * Created by ajcra on 1/25/2018.
 */
class ProfileItem {
    var selected: Boolean
    var profile: Profile
    constructor(profile: Profile, selected: Boolean = false) {
        this.profile = profile
        this.selected = selected
    }
}
//should contain click listeners, can have different ViewHolders
class ProfileRecyclerAdapter: RecyclerView.Adapter<ProfileRecyclerAdapter.ViewHolder> {
    private var cfm: CloudFileManager
    private var dataset: ArrayList<Profile>
    private var selectable: Boolean
    private var clickSelect: Boolean
    private var big: Boolean

    var selectedSet = HashSet<Profile>()
        private set

    var onSelectChangeCallback: ((HashSet<Profile>) -> Unit)? = null

    constructor(cfm: CloudFileManager, dataset: ArrayList<Profile>, selectable: Boolean = false, clickSelect: Boolean = false, big: Boolean = false) {
        this.cfm = cfm
        this.dataset = dataset
        this.selectable = selectable
        this.clickSelect = clickSelect
        this.big = big
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view: View? = null
        if (big) {
            view = LayoutInflater.from(parent?.context).inflate(R.layout.row_profile, parent, false)
        } else {
            view = LayoutInflater.from(parent?.context).inflate(R.layout.row_profile, parent, false)
        }
        return ViewHolder(view)
    }

    fun removeProfile(id: String) {
        var i = dataset.indexOfFirst {
            id == it.id
        }
        var profile = dataset[i]
        selectedSet.remove(profile)
        dataset.removeAt(i)
        notifyItemRemoved(i)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var profile = dataset[holder!!.adapterPosition]
        if (selectable) {
            var selected = selectedSet.contains(profile)
            holder!!.animateSelected(selected)
            if (clickSelect) {
                holder!!.rootView.setOnClickListener {
                    if (selected) {
                        selectedSet.remove(profile)
                    } else {
                        selectedSet.add(profile)
                    }
                    selected = !selected
                    holder!!.animateSelected(selected)
                    onSelectChangeCallback?.invoke(selectedSet)
                }
            } else {
                holder!!.rootView.setOnLongClickListener {
                    if (selected) {
                        selectedSet.remove(profile)
                    } else {
                        selectedSet.add(profile)
                    }
                    selected = !selected
                    holder!!.animateSelected(selected)
                    onSelectChangeCallback?.invoke(selectedSet)
                    true
                }
            }
        } else {
            holder!!.rootView.setOnClickListener {
                var intent = Intent(holder!!.ctx, ProfileActivity::class.java)
                intent.putExtra("id", profile.id)
                holder!!.ctx.startActivity(intent)
            }
        }
        GlobalScope.launch(Dispatchers.Main) {
            holder!!.profNameView.text = profile.getName().await()
        }
        GlobalScope.launch(Dispatchers.IO) {
            var profilePicUrl = profile.getProfilePicUrl().await()
            if (profilePicUrl == null) {
                profilePicUrl = "profileImgs/blank-profile-picture-973460_640.png"
            }
            cfm.download(profilePicUrl, object : CloudFileListener() {
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
                        launch(Dispatchers.Main) {
                            holder!!.profImgView!!.setImageBitmap(bmap)
                        }
                    }
                }
            })
        }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    class ViewHolder : RecyclerView.ViewHolder {
        var profNameView: TextView
        var profImgView: ImageView
        var profLayout: LinearLayout
        var rootView: View
        var ctx: Context

        constructor(itemView: View)
                : super(itemView) {
            ctx = itemView.context
            rootView = itemView
            profNameView = itemView.findViewById(R.id.rowprof_profname)
            profImgView = itemView.findViewById(R.id.rowprof_profimg)
            profLayout = itemView.findViewById(R.id.rowprof_layout)
        }

        fun animateSelected(selected: Boolean) {
            if (!selected) {
                val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), Color.GREEN, Color.RED)
                colorAnimation.duration = 70 // milliseconds
                colorAnimation.addUpdateListener { animator -> profLayout.setBackgroundColor(animator.animatedValue as Int) }
                colorAnimation.start()
                rootView.animate()
                        .alpha(1f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(70)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {

                            }
                        })
            } else {
                val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), Color.RED, Color.GREEN)
                colorAnimation.duration = 70 // milliseconds
                colorAnimation.addUpdateListener { animator -> profLayout.setBackgroundColor(animator.animatedValue as Int) }
                colorAnimation.start()
                rootView.animate()
                        .alpha(.5f)
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(70)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {

                            }
                        })
            }
        }
    }
}