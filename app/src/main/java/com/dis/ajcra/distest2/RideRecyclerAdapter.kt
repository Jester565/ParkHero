package com.dis.ajcra.distest2

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
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File

//should contain click listeners, can have different ViewHolders
class RideRecyclerAdapter: RecyclerView.Adapter<RideRecyclerAdapter.ViewHolder> {
    private var cfm: CloudFileManager
    private var dataset: ArrayList<Ride>

    constructor(cfm: CloudFileManager, dataset: ArrayList<Ride>) {
        this.cfm = cfm
        this.dataset = dataset
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_ride, parent, false)
        var viewHolder = ViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        var ride = dataset.get(position)
        if (holder != null) {
            async(UI) {
                holder.nameView.text = ride.getName().await()
                var waitTime = ride.getWaitTime().await()
                if (waitTime != null) {
                    holder.waitTimeView.text = waitTime.toString()
                } else {
                    holder.waitTimeView.text = ride.getStatus().await()
                }
                var fpTime = ride.getFastPassTime().await()
                if (fpTime != null) {
                    holder.fastPassTimeView.text = fpTime.toString()
                } else {
                    holder.fastPassTimeView.text = "--"
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    class ViewHolder : RecyclerView.ViewHolder {
        var nameView: TextView
        var waitTimeView: TextView
        var fastPassTimeView: TextView
        var rootView: View
        var ctx: Context

        constructor(itemView: View)
                : super(itemView) {
            ctx = itemView.context
            rootView = itemView
            nameView = rootView.findViewById(R.id.rowride_name)
            waitTimeView = rootView.findViewById(R.id.rowride_waittime)
            fastPassTimeView = rootView.findViewById(R.id.rowride_fastpass)
        }
    }
}