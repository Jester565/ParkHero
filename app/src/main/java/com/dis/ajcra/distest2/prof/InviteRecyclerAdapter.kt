package com.dis.ajcra.distest2.prof

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import java.lang.Exception

/**
 * Created by ajcra on 1/25/2018.
 */
//should contain click listeners, can have different ViewHolders
class InviteRecyclerAdapter: RecyclerView.Adapter<InviteRecyclerAdapter.ViewHolder> {
    private var cfm: CloudFileManager
    private var dataset: ArrayList<Invite>

    constructor(cfm: CloudFileManager, dataset: ArrayList<Invite>) {
        this.cfm = cfm
        this.dataset = dataset
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_invite, parent, false)
        var viewHolder = ViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (dataset[holder!!.adapterPosition].isOwner) {
            holder!!.arrowImgView.visibility = View.VISIBLE
        } else {
            holder!!.acceptButton.visibility = View.VISIBLE
            holder!!.declineButton.visibility = View.VISIBLE
            holder!!.acceptButton.setOnClickListener {v ->
                async(UI) {
                    dataset[holder!!.adapterPosition].target.addFriend().await()
                    v.isEnabled = false
                    dataset[holder!!.adapterPosition].accept()
                    dataset.removeAt(holder!!.adapterPosition)
                    notifyItemRemoved(holder!!.adapterPosition)
                }
            }
            holder!!.declineButton.setOnClickListener {v ->
                async(UI) {
                    dataset[holder!!.adapterPosition].target.removeFriend().await()
                    v.isEnabled = false
                    dataset[holder!!.adapterPosition].decline()
                    dataset.removeAt(holder!!.adapterPosition)
                    notifyItemRemoved(holder!!.adapterPosition)
                }
            }
        }
        holder!!.rootView.setOnClickListener {
            var profile = dataset[holder!!.adapterPosition].target
            var intent = Intent(holder!!.ctx, ProfileActivity::class.java)
            intent.putExtra("id", profile.id)
            holder!!.ctx.startActivity(intent)
        }
        async(UI) {
            holder!!.profNameView.text = dataset[position].target.getName().await()
            var inviteStatus = dataset[position].target.getInviteStatus().await()
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
            var profilePicUrl = dataset[position].target.getProfilePicUrl().await()
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
        var acceptButton: ImageButton
        var declineButton: ImageButton
        var arrowImgView: ImageView
        var rootView: View
        var ctx: Context

        constructor(itemView: View)
                : super(itemView) {
            ctx = itemView.context
            rootView = itemView
            profNameView = itemView.findViewById(R.id.rowinv_profname)
            profImgView = itemView.findViewById(R.id.rowinv_profimg)
            acceptButton = itemView.findViewById(R.id.rowinv_acceptButton)
            declineButton = itemView.findViewById(R.id.rowinv_declineButton)
            arrowImgView = itemView.findViewById(R.id.rowinv_arrow)
        }
    }
}