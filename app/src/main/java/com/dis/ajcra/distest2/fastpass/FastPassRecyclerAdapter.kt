package com.dis.ajcra.distest2.fastpass

/*
class FastPassRecyclerAdapter(private var schedules: List<DisFastPass>, private var maxBlockLevel: Int, private var cfm: CloudFileManager): RecyclerView.Adapter<ScheduleRecyclerAdapter.ViewHolder>()
{
    private var inputHourFormat: SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    private var outputHourFormat: SimpleDateFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ScheduleRecyclerAdapter.ViewHolder {
        var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_schedule, parent, false)
        var viewHolder = ScheduleRecyclerAdapter.ViewHolder(view)
        return viewHolder
    }

    override fun getItemCount(): Int {
        return schedules.size
    }

    override fun onBindViewHolder(holder: ScheduleRecyclerAdapter.ViewHolder?, position: Int) {
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
            async {

                cfm.download(schedule.parkIconUrl()!!, object : CloudFileListener() {
                    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                        Log.d("STATE", "ParkIcon downloading")
                    }

                    override fun onComplete(id: Int, file: File) {
                        async(UI) {
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
*/