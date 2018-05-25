package com.dis.ajcra.distest2

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.support.v4.graphics.ColorUtils
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.fastpass.fragment.DisRide
import com.dis.ajcra.fastpass.fragment.DisRideTime
import com.dis.ajcra.fastpass.fragment.DisRideUpdate
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

    fun updateRide(update: DisRideUpdate): Boolean {
        var rideIdx = dataset.indexOfFirst {
            it.id == update.id()
        }
        if (rideIdx != null) {
            dataset.get(rideIdx).time  = update.time()!!.fragments().disRideTime()
            this.notifyItemChanged(rideIdx)
            return true
        }
        return false
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        var ride = dataset.get(position)
        if (holder != null) {
            async(UI) {
                holder!!.rootView.setOnClickListener {
                    var rideID = ride.id
                    var intent = Intent(holder!!.ctx, RideActivity::class.java)
                    intent.putExtra("id", rideID)
                    holder!!.ctx.startActivity(intent)
                }
                holder.nameView.text = ride.info.name()!!
                var waitTime = ride.time?.waitTime()
                if (waitTime != null) {
                    holder.waitTimeView.text = waitTime.toString()
                } else {
                    holder.waitTimeView.text = ride.time?.status()
                }
                var fpTime = ride.time?.fastPassTime()
                if (fpTime != null) {
                    holder.fastPassTimeView.text = fpTime
                } else {
                    holder.fastPassTimeView.text = "--"
                }
                var waitRating = ride.time?.waitRating()?.toFloat()
                if (waitRating != null) {
                    if (waitRating < -10.0f) {
                        waitRating = -10.0f
                    } else if (waitRating > 10.0f) {
                        waitRating = 10.0f
                    }
                    var hsl = floatArrayOf((waitRating + 10.0f)/20.0f * 120.0f, 1.0f, 0.5f)
                    holder.bgLayout.setBackgroundColor(ColorUtils.HSLToColor(hsl))
                }
            }
            var picUrl = ride.info.picUrl()
            if (picUrl != null && holder.imgKey != picUrl)
            {
                holder.imgKey = picUrl
                async {
                    picUrl = picUrl?.substring(0, picUrl?.length!! - 4) + "-0" + picUrl?.substring(picUrl?.length!! - 4)
                    Log.d("STATE", "PICURL: " + picUrl)
                    cfm.download(picUrl.toString(), object : CloudFileListener() {
                        override fun onError(id: Int, ex: Exception?) {
                            Log.d("STATE", "RidePicUrlErr: " + ex?.message)
                        }

                        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                            //Log.d("STATE", "ProgressChanged")
                        }

                        override fun onStateChanged(id: Int, state: TransferState?) {
                            //Log.d("STATE", " STATE CHANGE: " + state.toString())
                        }

                        override fun onComplete(id: Int, file: File) {
                            Log.d("STATE", "Ride download complete")
                            async(UI) {
                                holder.imgView.setImageURI(Uri.fromFile(file))
                            }
                        }
                    })
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
        var bgLayout: RelativeLayout
        var imgView: ImageView
        var imgKey: String? = null

        constructor(itemView: View)
                : super(itemView) {
            ctx = itemView.context
            rootView = itemView
            imgView = rootView.findViewById(R.id.rowride_img)
            bgLayout = rootView.findViewById(R.id.rowride_layout)
            nameView = rootView.findViewById(R.id.rowride_name)
            waitTimeView = rootView.findViewById(R.id.rowride_waittime)
            fastPassTimeView = rootView.findViewById(R.id.rowride_fastpass)
        }
    }
}