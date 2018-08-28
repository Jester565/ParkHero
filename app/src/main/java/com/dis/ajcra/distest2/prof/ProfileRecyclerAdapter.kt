package com.dis.ajcra.distest2.prof

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import java.lang.Exception
import android.animation.ValueAnimator
import android.widget.LinearLayout
import com.dis.ajcra.distest2.R


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
    private var dataset: ArrayList<ProfileItem>
    private var selectable: Boolean

    constructor(cfm: CloudFileManager, dataset: ArrayList<ProfileItem>, selectable: Boolean = false) {
        this.cfm = cfm
        this.dataset = dataset
        this.selectable = selectable
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_profile, parent, false)
        var viewHolder = ViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        if (selectable) {
            holder!!.animateSelected(dataset[position].selected)
            holder!!.rootView.setOnClickListener {
                dataset[position].selected = !dataset[position].selected
                holder!!.animateSelected(dataset[position].selected)
            }
        } else {
            holder!!.rootView.setOnClickListener {
                var profile = dataset[holder!!.adapterPosition].profile
                var intent = Intent(holder!!.ctx, ProfileActivity::class.java)
                intent.putExtra("id", profile.id)
                holder!!.ctx.startActivity(intent)
            }
        }
        async(UI) {
            holder!!.profNameView.text = dataset[position].profile.getName().await()
            var inviteStatus = dataset[position].profile.getInviteStatus().await()
            if (inviteStatus == 1) {
                holder!!.rootView.setBackgroundColor(Color.YELLOW)
            } else if (inviteStatus == 2) {
                holder!!.rootView.setBackgroundColor(Color.CYAN)
            } else if (inviteStatus == 3) {
                holder!!.rootView.setBackgroundColor(Color.GREEN)
            }
            Log.d("STATE", "Name set: " + holder!!.profNameView.text)
        }
        async {
            var profilePicUrl = dataset[position].profile.getProfilePicUrl().await()
            if (profilePicUrl == null) {
                profilePicUrl = "profileImgs/blank-profile-picture-973460_640.png"
            }
            cfm.download(profilePicUrl, object : CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {

                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    async(UI) {

                    }
                }

                override fun onStateChanged(id: Int, state: TransferState?) {
                    async(UI) {

                    }
                }

                override fun onComplete(id: Int, file: File) {
                    async {
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
                        async(UI) {
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