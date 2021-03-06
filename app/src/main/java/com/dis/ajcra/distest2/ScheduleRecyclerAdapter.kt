package com.dis.ajcra.distest2

import android.graphics.Color
import android.net.Uri
import android.support.v4.graphics.ColorUtils
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.fastpass.GetSchedulesQuery
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScheduleRecyclerAdapter(private var schedules: List<GetSchedulesQuery.Schedule>, private var maxBlockLevel: Int, private var cfm: CloudFileManager): RecyclerView.Adapter<ScheduleRecyclerAdapter.ViewHolder>()
{
    private var inputHourFormat: SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    private var outputHourFormat: SimpleDateFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_schedule, parent, false)
        var viewHolder = ScheduleRecyclerAdapter.ViewHolder(view)
        return viewHolder
    }

    override fun getItemCount(): Int {
        return schedules.size
    }

    override fun onBindViewHolder(holder: ScheduleRecyclerAdapter.ViewHolder, position: Int) {
        if (holder != null) {
            var schedule = schedules.get(position)
            Log.d("STATE", "Binding view holder for: " + schedule.parkName())
            holder.nameView.text = schedule.parkName()
            holder.hourView.text = genHourStr(schedule.openTime()!!, schedule.closeTime()!!)

            if (schedule.magicStartTime() != null) {
                holder.magicHourLayout.visibility = View.VISIBLE
                holder.magicHourView.text = genHourStr(schedule.magicStartTime()!!, schedule.magicEndTime()!!)
            }

            var borderColor = Color.BLACK
            if (schedule.blockLevel() != null && schedule.blockLevel()!! <= maxBlockLevel) {
                if (schedule.crowdLevel() != null) {
                    //Create color from the crowdLevel (can be 0,1,2,3)
                    var hsl = floatArrayOf((schedule.crowdLevel()!!.toFloat()) / ParkScheduleFragment.CROWDLEVEL_MAX.toFloat() * 120.0f, 1.0f, 0.5f)
                    borderColor = ColorUtils.HSLToColor(hsl)
                } else {
                    //unknown crowd, show blue
                    borderColor = Color.BLUE
                }
            }
            holder.iconView.borderColor = borderColor

            Log.d("STATE", "ParkIcon: " + schedule.parkIconUrl()!!)
            GlobalScope.async(Dispatchers.IO) {
                cfm.download(schedule.parkIconUrl()!!, object : CloudFileListener() {
                    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                        Log.d("STATE", "ParkIcon downloading")
                    }

                    override fun onComplete(id: Int, file: File) {
                        GlobalScope.async(Dispatchers.Main) {
                            holder.iconView.setImageURI(Uri.fromFile(file))
                        }
                    }

                    override fun onError(id: Int, ex: Exception?) {
                        Log.e("STATE", "ERROR downloading icon: " + ex!!.message)
                    }
                })
            }
        }
    }

    fun setMaxBlockLevel(maxBlockLevel: Int) {
        this.maxBlockLevel = maxBlockLevel
    }

    private fun genHourStr(startHour: String, endHour: String): String {
        var hourStr = outputHourFormat.format(inputHourFormat.parse(startHour))
        hourStr += " to "
        hourStr += outputHourFormat.format(inputHourFormat.parse(endHour))
        return hourStr
    }

    class ViewHolder : RecyclerView.ViewHolder {
        var iconView: CircleImageView
        var nameView: TextView
        var hourView: TextView
        var magicHourLayout: LinearLayout
        var magicHourView: TextView

        constructor(rootView: View)
                : super(rootView) {
            iconView = rootView.findViewById(R.id.rowschedule_img)
            nameView = rootView.findViewById(R.id.rowschedule_name)
            hourView = rootView.findViewById(R.id.rowschedule_hours)
            magicHourLayout = rootView.findViewById(R.id.rowschedule_magicHoursLayout)
            magicHourView = rootView.findViewById(R.id.rowschedule_magicHours)
        }
    }
}