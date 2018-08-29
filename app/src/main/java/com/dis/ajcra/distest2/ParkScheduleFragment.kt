package com.dis.ajcra.distest2

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.graphics.ColorUtils
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.fastpass.GetSchedulesQuery
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.text.SimpleDateFormat
import java.util.*

class ParkDayDecorator: DayViewDecorator {
    private var colors: ArrayList<Int>
    private var days = HashSet<CalendarDay>()
    private var background: BarDrawable
    private var textAppearanceSpan: TextAppearanceSpan? = null

    constructor(colors: ArrayList<Int>, context: Context? = null) {
        this.colors = colors
        background = BarDrawable(colors)
        if (context != null) {
            textAppearanceSpan = TextAppearanceSpan(context, android.R.style.TextAppearance_DeviceDefault_Large)
        }
    }

    fun addDay(date: Long) {
        days.add(CalendarDay.from(date))
    }

    override fun shouldDecorate(day: CalendarDay?): Boolean {
        return (days.contains(day!!))
    }

    override fun decorate(view: DayViewFacade?) {
        if (view != null) {
            view.setBackgroundDrawable(background)
            view.addSpan(ForegroundColorSpan(Color.WHITE))
            if (textAppearanceSpan != null) {
                view.addSpan(textAppearanceSpan!!)
            }
        }
    }
}

class ParkScheduleFragment : Fragment() {
    companion object {
        fun GetInstance(): ParkScheduleFragment {
            val fragment = ParkScheduleFragment()
            return fragment
        }

        var CROWDLEVEL_MAX = 3

        fun GenEID(parkSchedules: List<GetSchedulesQuery.Schedule>, maxBlockLevel: Int): Int {
            var decKey = 0
            var i = 0
            for (parkSchedule in parkSchedules) {
                if (parkSchedule.blockLevel() != null && parkSchedule.blockLevel()!! <= maxBlockLevel) {
                    if (parkSchedule.crowdLevel() != null) {
                        //we add 2 here to reserve space for the blocked out and unknown crowd level colors
                        decKey += i * (CROWDLEVEL_MAX + 2) + (parkSchedule.crowdLevel()!! + 2)
                    } else {
                        decKey += i * (CROWDLEVEL_MAX + 2) + 1
                    }
                } else {
                    decKey += i * (CROWDLEVEL_MAX + 2)
                }
                i++
            }
            return decKey
        }

        fun GenColor(schedule: GetSchedulesQuery.Schedule, maxBlockLevel: Int): Int {
            var color = Color.BLACK
            if (schedule.blockLevel() != null && schedule.blockLevel()!! <= maxBlockLevel) {
                if (schedule.crowdLevel() != null) {
                    //Create color from the crowdLevel (can be 0,1,2,3)
                    var hsl = floatArrayOf((schedule.crowdLevel()!!.toFloat()) / ParkScheduleFragment.CROWDLEVEL_MAX.toFloat() * 120.0f, 1.0f, 0.5f)
                    color = ColorUtils.HSLToColor(hsl)
                } else {
                    //unknown crowd, show blue
                    color = Color.BLUE
                }
            }
            return color
        }

        var PassTypes: ArrayList<String> = arrayListOf("Southern California Select", "Southern California", "Deluxe", "Signature", "Signature Plus")
    }

    private lateinit var calendar: MaterialCalendarView
    private lateinit var dateView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var hourlyWeatherFragment: HourlyWeatherFragment

    private var scheduleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
    private var convertDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
    private var displayDateFormat: SimpleDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy")

    private var schedules = TreeMap<Long, ArrayList<GetSchedulesQuery.Schedule>>()
    private var dateDecorators = HashMap<Int, ParkDayDecorator>()
    private var selectedDecorator: ParkDayDecorator? = null
    private var maxBlockLevel: Int = 1

    private lateinit var cfm: CloudFileManager
    private lateinit var appSync: AppSyncTest
    private lateinit var scheduleAdapter: ScheduleRecyclerAdapter
    private var adapterData = ArrayList<GetSchedulesQuery.Schedule>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cfm = CloudFileManager(CognitoManager.GetInstance(activity!!.applicationContext), activity!!.applicationContext)
        appSync = AppSyncTest.GetInstance(CognitoManager.GetInstance(activity!!.applicationContext), activity!!.applicationContext)
        scheduleAdapter = ScheduleRecyclerAdapter(adapterData, maxBlockLevel, cfm)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        var rootView = inflater!!.inflate(R.layout.fragment_park_schedule, container, false)

        calendar = rootView.findViewById(R.id.parkschedule_calendar)
        calendar.arrowColor = Color.WHITE

        dateView = rootView.findViewById(R.id.parkschedule_date)

        recyclerView = rootView.findViewById(R.id.parkschedule_recycler)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = scheduleAdapter
        recyclerView.setItemViewCacheSize(200)
        recyclerView.isDrawingCacheEnabled = true

        hourlyWeatherFragment = HourlyWeatherFragment.GetInstance()
        childFragmentManager.beginTransaction().replace(R.id.parkschedule_weatherLayout, hourlyWeatherFragment).commit()

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("STATE", "ParkSchedule onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        appSync.getSchedules(object: AppSyncTest.GetSchedulesCallback {
            override fun onResponse(data: List<GetSchedulesQuery.Schedule>) {
                activity!!.runOnUiThread {
                    schedules.clear()

                    var date: Long = 0
                    //Group multiple parkSchedules for the same date into the same array
                    var parkScheduleArr: ArrayList<GetSchedulesQuery.Schedule>? = null
                    for (parkSchedule in data) {
                        var newDate = scheduleDateFormat.parse(parkSchedule.date()).time
                        //If we're seeing a new date, save the last parkSchedule array and create a new one
                        if (date != newDate || parkScheduleArr == null) {
                            if (parkScheduleArr != null) {
                                schedules[date] = parkScheduleArr
                            }
                            date = newDate
                            parkScheduleArr = ArrayList()
                        }
                        parkScheduleArr!!.add(parkSchedule)
                    }
                    if (parkScheduleArr != null && parkScheduleArr.size > 0) {
                        schedules[date] = parkScheduleArr
                    }
                    displaySchedules()
                }
            }

            override fun onError(ec: Int?, msg: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
    }

    fun setMaxBlockLevel(blockLevel: Int) {
        maxBlockLevel = blockLevel
        scheduleAdapter.setMaxBlockLevel(blockLevel)
        if (schedules.size > 0) {
            displaySchedules()
        }
    }

    private fun displaySchedules() {
        calendar.removeDecorators()
        dateDecorators.clear()

        //Restrict the calendar to the dates we have
        var firstDate: Long = schedules!!.firstEntry().key
        var lastDate: Long = schedules!!.lastEntry().key
        calendar.state().edit().setMinimumDate(CalendarDay.from(firstDate)).setMaximumDate(CalendarDay.from(lastDate)).commit()

        //Schedules have to be converted into DayViewDecorators. Some days may have the same schedule,
        //so we reuse their DayViewDecorator
        for (entry in schedules!!) {
            var scheduleDate = entry.key
            var parkSchedules = entry.value
            var decKey = GenEID(parkSchedules, maxBlockLevel)
            var colors = ArrayList<Int>()
            for (parkSchedule in parkSchedules) {
                colors.add(GenColor(parkSchedule, maxBlockLevel))
            }
            var decorator = dateDecorators.get(decKey)
            if (decorator == null) {
                decorator = ParkDayDecorator(colors)
                dateDecorators[decKey] = decorator
            }
            decorator!!.addDay(scheduleDate)
        }

        for (entry in dateDecorators) {
            calendar.addDecorator(entry.value)
        }

        //Refresh adapters to reflect calendar changes
        if (adapterData.size > 0) {
            var date = scheduleDateFormat.parse(adapterData.first().date()!!)
            scheduleAdapter.notifyDataSetChanged()

            //If there are items in the adapter, a date must be selected
            decorateSelected(date.time, adapterData)
        } else {
            selectDay(firstDate)
        }

        calendar.setOnDateChangedListener { calendar, day, selected ->
            Log.d("STATE", "SetOnDateChange call")
            //truncate to begginning of day
            var date = convertDateFormat.parse(convertDateFormat.format(day.date))
            selectDay(date!!.time)
        }
    }

    private fun selectDay(date: Long) {
        var parkSchedules = schedules[date]
        dateView.text = displayDateFormat.format(date)
        if (parkSchedules != null) {
            decorateSelected(date, parkSchedules)

            adapterData.clear()
            adapterData.addAll(parkSchedules)
            scheduleAdapter.notifyDataSetChanged()

            hourlyWeatherFragment.getWeather(date)
        } else {
            Log.w("STATE", "Could not find schedules for date selected")
        }
    }

    private fun decorateSelected(date: Long, parkSchedules: List<GetSchedulesQuery.Schedule>) {
        if (parkSchedules.isNotEmpty()) {
            var colors = ArrayList<Int>()
            for (parkSchedule in parkSchedules) {
                colors.add(GenColor(parkSchedule, maxBlockLevel))
            }
            if (selectedDecorator != null) {
                calendar.removeDecorator(selectedDecorator)
            }
            selectedDecorator = ParkDayDecorator(colors, context)
            selectedDecorator!!.addDay(date)
            calendar.addDecorator(selectedDecorator)
        }
    }
}