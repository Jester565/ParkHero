package com.dis.ajcra.distest2

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.fastpass.GetHourlyWeatherQuery
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.text.SimpleDateFormat
import java.util.*



class HourlyWeatherFragment : Fragment() {
    companion object {
        fun GetInstance(): HourlyWeatherFragment {
            val fragment = HourlyWeatherFragment()
            return fragment
        }
    }


    private lateinit var weatherChart: LineChart
    private lateinit var weatherProgress: ProgressBar
    private lateinit var weatherNoData: TextView

    private lateinit var appSync: AppSyncTest

    private var hourDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSync = AppSyncTest.GetInstance(CognitoManager.GetInstance(activity!!.applicationContext), activity!!.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        var rootView = inflater!!.inflate(R.layout.fragment_hourly_weather, container, false)
        weatherChart = rootView.findViewById(R.id.hourlyweather_tempChart)
        weatherProgress = rootView.findViewById(R.id.hourlyweather_graphProgress)
        weatherNoData = rootView.findViewById(R.id.hourlyweather_nodata)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("STATE", "ParkSchedule onViewCreated")
        super.onViewCreated(view, savedInstanceState)
    }

    fun getWeather(date: Long) {
        weatherChart.visibility = View.GONE
        weatherNoData.visibility = View.GONE
        weatherProgress.visibility = View.VISIBLE

        var dateStr = hourDateFormat.format(date)
        appSync.getHourlyWeather(dateStr, object: AppSyncTest.GetHourlyWeatherCallback {
            override fun onResponse(data: List<GetHourlyWeatherQuery.Weather>) {
                async(UI) {
                    Log.d("STATE: ", "Hourly weather response")
                    weatherProgress.visibility = View.GONE
                    if (displayWeather(data)) {
                        weatherChart.visibility = View.VISIBLE
                    } else {
                        Log.w("STATE", "Display weather graph failed")
                        weatherNoData.visibility = View.VISIBLE
                    }
                }
            }

            override fun onError(ec: Int?, msg: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
    }

    fun displayWeather(hourlyWeathers: List<GetHourlyWeatherQuery.Weather>): Boolean {
        var tempDataset = genChartDps(hourlyWeathers, "Feels Like Fahrenheit", Color.rgb(200, 200, 200), Color.WHITE)
        if (tempDataset != null) {
            var xAxisFormatter = HourlyWeatherFragment.HourAxisTimeValueFormatter()
            weatherChart.xAxis.setValueFormatter(xAxisFormatter)
            var waitTimeMarkerView = HourlyWeatherFragment.WaitTimeMarkerView(getContext()!!.applicationContext, R.layout.wait_time_marker)
            weatherChart.marker = waitTimeMarkerView
            weatherChart.axisLeft.textColor = Color.WHITE
            weatherChart.axisRight.textColor = Color.WHITE
            weatherChart.description.text = ""
            weatherChart.xAxis.textColor = Color.WHITE
            weatherChart.legend.textColor = Color.WHITE
            var setArr = ArrayList<ILineDataSet>()
            setArr.add(tempDataset)
            weatherChart.xAxis.axisMinimum = 9f * 100f
            weatherChart.data = LineData(setArr)
            weatherChart.invalidate()
            return true
        }
        return false
    }

    fun genChartDps(hourlyWeathers: List<GetHourlyWeatherQuery.Weather>, label: String, color: Int, textColor: Int): LineDataSet? {
        var graphEntries = ArrayList<Entry>()

        var cal = GregorianCalendar.getInstance()
        var lastDow: Int? = null
        for (weather in hourlyWeathers) {
            if (weather.dateTime() != null && weather.feelsLikeF() != null) {
                var date = hourDateFormat.parse(weather.dateTime())
                cal.time = date
                var dow = cal.get(Calendar.DAY_OF_WEEK)
                var hour = cal.get(Calendar.HOUR_OF_DAY)
                var mins = cal.get(Calendar.MINUTE)
                if (lastDow != null && lastDow != dow) {
                    hour += 24
                }
                graphEntries.add(Entry(hour.toFloat() * 100f + (mins.toFloat()/60f) * 100f, weather.feelsLikeF()!!.toFloat()))
            }
        }
        if (graphEntries.size != 0) {
            var dataSet = LineDataSet(graphEntries, label)
            dataSet.setColor(color)
            dataSet.valueTextColor = textColor
            dataSet.fillDrawable = ContextCompat.getDrawable(context!!, R.drawable.fade_white)
            dataSet.setDrawFilled(true)
            dataSet.setCircleColorHole(0x555555)
            return dataSet
        }
        return null
    }

    class HourAxisTimeValueFormatter: IAxisValueFormatter {
        fun formatVal(value: Float): String {
            var hour = (value/100f).toInt()
            var mins = (60*(value.toInt() % 100))/100
            var hourFormatted = (hour % 12)
            if (hourFormatted == 0) {
                hourFormatted = 12
            }
            var minStr = mins.toString()
            if (minStr.length <= 1) {
                minStr = "0" + minStr
            }
            var timeMark = "pm"
            if (hour % 24 < 12) {
                timeMark = "am"
            }
            return hourFormatted.toString() + ":" + minStr + " " + timeMark
        }

        override fun getFormattedValue(value: Float, axis: AxisBase?): String {
            try {
                return formatVal(value)
            } catch(ex: Exception) {
                Log.d("STATE", "EXCEPTION: " + ex.message)
            }
            return "--"
        }
    }

    class WaitTimeMarkerView: MarkerView {
        var textContent: TextView
        constructor(ctx: Context, layoutResource: Int)
                :super(ctx, layoutResource)
        {
            textContent = findViewById(R.id.waitmarker_text)
        }

        fun formatVal(value: Float): String {
            var hour = (value/100f).toInt()
            var mins = (60*(value.toInt() % 100))/100
            var hourFormatted = (hour % 12)
            if (hourFormatted == 0) {
                hourFormatted = 12
            }
            var minStr = mins.toString()
            if (minStr.length <= 1) {
                minStr = "0" + minStr
            }
            var timeMark = "pm"
            if (hour % 24 < 12) {
                timeMark = "am"
            }
            return hourFormatted.toString() + ":" + minStr + " " + timeMark
        }

        override fun refreshContent(e: Entry, highlight: Highlight) {
            var xStr = formatVal(e.x)
            var str = e.y.toString() + 0x00B0.toChar() + " at " + xStr
            //Log.d("STATE", "UPDATING STR: " + str)
            textContent.text = str
            this.offset.x = -width.toFloat()/2
            this.offset.y = -height.toFloat()
            super.refreshContent(e, highlight)
        }

    }
}