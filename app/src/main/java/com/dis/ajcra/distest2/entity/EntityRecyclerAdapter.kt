package com.dis.ajcra.distest2.entity

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.ProfileActivity
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File


//should contain click listeners, can have different ViewHolders
class EntityRecyclerAdapter: RecyclerView.Adapter<EntityRecyclerAdapter.ViewHolder> {
    private var cfm: CloudFileManager
    private var dataset: ArrayList<Entity>

    constructor(cfm: CloudFileManager, dataset: ArrayList<Entity>) {
        this.cfm = cfm
        this.dataset = dataset
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_entity, parent, false)
        var viewHolder = ViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var entity = dataset[position]
        holder!!.profImgView.setOnClickListener {
            var profile = dataset[holder!!.adapterPosition].owner
            var intent = Intent(holder!!.ctx, ProfileActivity::class.java)
            intent.putExtra("id", profile.id)
            holder!!.ctx.startActivity(intent)
        }
        async(UI) {
            holder!!.profNameView.setText(entity.owner.getName().await())

        }
        async {
            var profilePicUrl = entity.owner.getProfilePicUrl().await()
            if (profilePicUrl == null) {
                profilePicUrl = "profileImgs/blank-profile-picture-973460_640.png"
            }
            cfm.download(profilePicUrl, object : CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {

                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

                }

                override fun onStateChanged(id: Int, state: TransferState?) {

                }

                override fun onComplete(id: Int, file: File) {
                    async {
                        var options = BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeFile(file.absolutePath, options)
                        var imgScale = 1
                        while (options.outWidth / imgScale > 1000) {
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
            }, null, true)

            cfm.download(entity.id, object : CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {}
                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
                override fun onStateChanged(id: Int, state: TransferState?) {}

                override fun onComplete(id: Int, file: File) {
                    async {
                        var options = BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeFile(file.absolutePath, options)
                        var imgScale = 1
                        while (options.outWidth / imgScale > 1000) {
                            imgScale *= 2
                        }
                        options.inJustDecodeBounds = false
                        options.inSampleSize = imgScale
                        var bmap = BitmapFactory.decodeFile(file.absolutePath, options)
                        async(UI) {
                            holder!!.imgView!!.setImageBitmap(bmap)
                        }
                    }
                }
            }, entity.presignedUrl)
        }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    class ViewHolder : RecyclerView.ViewHolder {
        var profNameView: TextView
        var profImgView: ImageView
        var imgView: ImageView
        var rootView: View
        var ctx: Context

        constructor(itemView: View)
                : super(itemView) {
            ctx = itemView.context
            rootView = itemView
            profNameView = itemView.findViewById(R.id.rowentity_profname)
            profImgView = itemView.findViewById(R.id.rowentity_profimgview)
            imgView = itemView.findViewById(R.id.rowentity_imgview)
        }
    }
}