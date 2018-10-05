package com.dis.ajcra.distest2.fastpass

import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.ride.CRInfo
import com.dis.ajcra.distest2.ride.RideManager
import com.dis.ajcra.fastpass.fragment.DisFastPassTransaction
import kotlinx.coroutines.experimental.async
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class FastPassRecyclerAdapter(private var transactions: List<DisFastPassTransaction>, private var rideManager: RideManager, private var cfm: CloudFileManager): RecyclerView.Adapter<FastPassRecyclerAdapter.ViewHolder>()
{
    private var fpDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
    private var fpDateFormat2: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
    private var outputHourFormat: SimpleDateFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FastPassRecyclerAdapter.ViewHolder {
        var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_fastpasstrans, parent, false)
        var viewHolder = FastPassRecyclerAdapter.ViewHolder(view)
        return viewHolder
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onBindViewHolder(holder: FastPassRecyclerAdapter.ViewHolder, position: Int) {
        if (holder != null) {
            var transaction = transactions.get(position)
            var fpDate = parseFpDate(transaction.fpDT()!!)
            holder.startTimeView.text = outputHourFormat.format(fpDate)
            var diff = fpDate.time - Date().time
            var till = true
            if (diff < 0) {
                //add an hour to switch to fast pass ending time
                diff += 60 * 60 * 1000
                till = false
            }
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            var hoursFlt = hours.toFloat() + (minutes % 60).toFloat()/60f

            var tillStr = ""
            if (hours > 2) {
                tillStr += "~" + Math.abs(hoursFlt).roundToInt().toString() + " hours "
            } else if (hours > 0) {
                tillStr += Math.abs(hours).toString() + " hours, " + Math.abs(minutes % 60).toString() + " minutes "
            } else {
                tillStr += Math.abs(minutes % 60).toString() + " minutes "
            }
            if (till) {
                tillStr += "until ready"
            } else if (diff > 0) {
                tillStr += "until expiration"
            } else {
                tillStr += "late!"
            }
            holder.timeTillView.text = tillStr

            var rideID = transaction.rideID()!!
            rideManager.getRide(rideID, object : RideManager.GetRideCB {
                override fun onUpdate(ride: CRInfo) {
                    holder.rideNameView.text = ride.name
                    var picUrl = ride.picURL
                    if (picUrl != null && !holder.imgSet) {
                        holder.imgSet = true
                        async {
                            picUrl = ride.picURL?.substring(0, picUrl?.length!! - 4) + "-2" + picUrl?.substring(picUrl?.length!! - 4)
                            cfm.download(picUrl!!, object : CloudFileListener() {
                                override fun onComplete(id: Int, file: File) {
                                    super.onComplete(id, file)
                                    holder.imgView.setImageURI(Uri.fromFile(file))
                                }
                            })
                        }
                    }
                }

                override fun onFinalUpdate(ride: CRInfo) {

                }
            })
        }
    }

    private fun parseFpDate(str: String): Date {
        try {
            return fpDateFormat.parse(str)
        } catch (ex: ParseException) {
            return fpDateFormat2.parse(str)
        }
    }

    class ViewHolder : RecyclerView.ViewHolder {
        var imgView: ImageView
        var rideNameView: TextView
        var startTimeView: TextView
        var timeTillView: TextView

        var imgSet = false

        constructor(rootView: View)
                : super(rootView) {
            imgView = rootView.findViewById(R.id.rowfastpasstrans_rideImage)
            rideNameView = rootView.findViewById(R.id.rowfastpasstrans_rideName)
            startTimeView = rootView.findViewById(R.id.rowfastpasstrans_startTime)
            timeTillView = rootView.findViewById(R.id.rowfastpasstrans_timeTill)
        }
    }
}