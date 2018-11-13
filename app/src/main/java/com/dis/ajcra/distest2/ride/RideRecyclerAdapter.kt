package com.dis.ajcra.distest2.ride

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.support.v4.graphics.ColorUtils
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


//should contain click listeners, can have different ViewHolders
class RideRecyclerAdapter: RecyclerView.Adapter<RideRecyclerAdapter.ViewHolder> {
    private var cfm: CloudFileManager
    private var rides: ArrayList<CRInfo>
    private var pinnedRides: ArrayList<CRInfo>
    private var selectedRideIDs: HashSet<String>
    private var clickUpdateSubscriptions = HashMap<UUID, (Int, Boolean) -> Unit>()
    private var fpParseFormat = SimpleDateFormat("HH:mm:ss")
    private var dateDispFormat = SimpleDateFormat("h:mm a")

    constructor(cfm: CloudFileManager, rides: ArrayList<CRInfo>, pinnedRides: ArrayList<CRInfo>, selectedRideIDs: HashSet<String>) {
        this.cfm = cfm
        this.rides = rides
        this.pinnedRides = pinnedRides
        this.selectedRideIDs = selectedRideIDs
    }

    fun subscribeToClicks(cb: (Int, Boolean) -> Unit): UUID {
        var subID = UUID.randomUUID()
        clickUpdateSubscriptions.put(subID, cb)
        return subID
    }

    fun unsubscribeFromClicks(subID: UUID) {
        clickUpdateSubscriptions.remove(subID)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_ride, parent, false)
        var viewHolder = ViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        lateinit var ride: CRInfo
        if (position < pinnedRides.size) {
            ride = pinnedRides.get(position)
        } else {
            ride = rides.get(position - pinnedRides.size)
        }

        //HANDLE CLICKS
        holder.rootView.setOnClickListener {
            clickUpdateSubscriptions.forEach { uuid, cb ->
                cb.invoke(position, false)
            }
        }
        holder!!.rootView.setOnLongClickListener {
            clickUpdateSubscriptions.forEach { uuid, cb ->
                cb.invoke(position, true)
            }
            true
        }

        //SETUP DISPLAY
        holder.nameView.text = ride.name
        var waitTime = ride.waitTime
        if (waitTime != null) {
            holder.waitTimeView.text = waitTime.toString()
        } else {
            holder.waitTimeView.text = ride.status
        }
        var fpTime = ride.fpTime
        if (fpTime != null) {
            var date = fpParseFormat.parse(ride.fpTime)
            var dispStr1 = dateDispFormat.format(date)
            holder.fastPassTimeView.text = dispStr1
        } else {
            holder.fastPassTimeView.text = "--"
        }
        var waitRating = ride.waitRating?.toFloat()
        var rateColor = Color.GRAY
        if (waitRating != null) {
            if (waitRating < -10.0f) {
                waitRating = -10.0f
            } else if (waitRating > 10.0f) {
                waitRating = 10.0f
            }
            var hsl = floatArrayOf((waitRating + 10.0f)/20.0f * 120.0f, 1.0f, 0.5f)
            rateColor = ColorUtils.HSLToColor(hsl)
        }
        holder.rateColor = rateColor
        //DRAW SELECTION
        if (selectedRideIDs.contains(ride.id)) {
            val lp = FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(13, 13, 13, 13)
            holder!!.bgLayout.layoutParams = lp
            holder!!.bgLayout.setBackgroundColor(Color.rgb(138, 200, 255))
        } else {
            holder.bgLayout.setBackgroundColor(rateColor)
        }
        var picUrl = ride.picURL
        if (picUrl != null && holder.imgKey != picUrl)
        {
            holder.imgKey = picUrl
            GlobalScope.async(Dispatchers.IO) {
                picUrl = picUrl?.substring(0, picUrl?.length!! - 4) + "-0" + picUrl?.substring(picUrl?.length!! - 4)
                Log.d("STATE", "PICURL: " + picUrl)
                cfm.download(picUrl.toString(), object : CloudFileListener() {
                    override fun onError(id: Int, ex: Exception?) {
                        Log.d("STATE", "RidePicUrlErr: " + ex?.message)
                    }

                    override fun onComplete(id: Int, file: File) {
                        Log.d("STATE", "Ride download complete")
                        GlobalScope.async(Dispatchers.Main) {
                            holder.imgView.setImageURI(Uri.fromFile(file))
                        }
                    }
                })
            }
        } else {
            holder.imgView.setImageResource(R.drawable.ic_cancel_black_24dp)
        }
    }

    override fun getItemCount(): Int {
        return pinnedRides.size + rides.size
    }

    class ViewHolder : RecyclerView.ViewHolder {
        var nameView: TextView
        var waitTimeView: TextView
        var fastPassTimeView: TextView
        var rootView: View
        var ctx: Context
        var bgLayout: RelativeLayout
        var imgView: CircleImageView
        var rateColor: Int? = null
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