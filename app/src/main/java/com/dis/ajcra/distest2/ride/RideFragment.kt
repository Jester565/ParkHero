package com.dis.ajcra.distest2.ride

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.AppSyncTest
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.fastpass.fragment.DisRideDP
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.*
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext


class RideFragment : CoroutineScope, Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var cfm: CloudFileManager
    private lateinit var rideManager: RideManager
    private lateinit var rideID: String
    private lateinit var waitTimeChart: LineChart
    private lateinit var progressBar: ProgressBar
    private lateinit var noDataText: TextView
    private lateinit var rideNameText: TextView
    private lateinit var rideWaitText: TextView
    private lateinit var rideFPLabel: TextView
    private lateinit var rideFPText: TextView
    private lateinit var rideFPSubLabel: TextView
    private lateinit var rideRatingText: TextView
    private lateinit var rideRatingSubLabel: TextView
    private lateinit var rideLastUpdatedLabel: TextView
    private lateinit var rideLastUpdatedText: TextView
    private lateinit var rideLastUpdatedSubLabel: TextView
    private lateinit var rideImg: ImageView
    private lateinit var minuteWaitLabel: TextView
    private var ratingFormat = DecimalFormat("#.#")
    private var fpParseFormat = SimpleDateFormat("HH:mm:ss")
    private var dateDispFormat = SimpleDateFormat("h:mm a")
    var dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private var infoSet: Boolean = false
    private var lastUpdateRefreshHandler: Handler? = null
    private var rideDPs: AppSyncTest.DisRideDPs? = null
    private var waitTime: Int? = null
    private var fastPassTime: String? = null
    private var lastUpdated: Long? = null

    private var displayLastUpdated = {
        var lastUpdatedTimestamp = lastUpdated
        if (lastUpdatedTimestamp != null) {
            rideLastUpdatedLabel.text = "Updated"
            var updatedMinsAgo = (Date().time - lastUpdatedTimestamp) / (1000 * 60)
            if (updatedMinsAgo == 0L) {
                rideLastUpdatedText.text = "Just Now"
                rideLastUpdatedSubLabel.text = ""
            } else {
                rideLastUpdatedText.text = updatedMinsAgo.toString()
                rideLastUpdatedSubLabel.text = "Minutes Ago"
            }
        }
    }

    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
        rideManager = RideManager.GetInstance(cognitoManager, context!!.applicationContext)

        rideID = arguments!!.getString(RIDEID_PARAM)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_ride, container, false)
        waitTimeChart = rootView.findViewById(R.id.ride_waitTimeChart)
        progressBar = rootView.findViewById(R.id.ride_graphProgress)
        noDataText = rootView.findViewById(R.id.ride_nodata)
        rideNameText = rootView.findViewById(R.id.ride_name)
        rideWaitText = rootView.findViewById(R.id.ride_waitMins)
        rideFPLabel = rootView.findViewById(R.id.ride_fpLabel)
        rideFPText = rootView.findViewById(R.id.ride_fpText)
        rideFPSubLabel = rootView.findViewById(R.id.ride_fpSubLabel)
        rideRatingText = rootView.findViewById(R.id.ride_waitRating)
        rideRatingSubLabel = rootView.findViewById(R.id.ride_waitRatingSubLabel)
        rideImg = rootView.findViewById(R.id.ride_img)
        minuteWaitLabel = rootView.findViewById(R.id.ride_minutewaitlabel)
        rideLastUpdatedLabel = rootView.findViewById(R.id.ride_lastUpdateLabel)
        rideLastUpdatedText = rootView.findViewById(R.id.ride_lastUpdate)
        rideLastUpdatedSubLabel = rootView.findViewById(R.id.ride_lastUpdateSubLabel)

        return rootView
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
            var str = e.y.toString() + " mins at " + xStr
            //Log.d("STATE", "UPDATING STR: " + str)
            textContent.text = str
            this.offset.x = -width.toFloat()/2
            this.offset.y = -height.toFloat()
            super.refreshContent(e, highlight)
        }

    }

    fun getChartDps(dps: List<DisRideDP>, label: String, color: Int, textColor: Int): LineDataSet? {
        var graphEntries = ArrayList<Entry>()
        var cal = GregorianCalendar.getInstance()
        var lastDow: Int? = null
        for (rideDP in dps) {
            if (rideDP.dateTime() != null) {
                var waitMins = rideDP.waitTime()
                if (waitMins != null) {
                    var date = dateParser.parse(rideDP.dateTime())
                    cal.time = date
                    var dow = cal.get(Calendar.DAY_OF_WEEK)
                    var hour = cal.get(Calendar.HOUR_OF_DAY)
                    var mins = cal.get(Calendar.MINUTE)
                    if (lastDow == null) {
                        lastDow = dow
                    }
                    if (lastDow != null && lastDow != dow) {
                        hour += 24
                    }
                    Log.d("RIDEDP", "X: " + (hour.toFloat() * 100f + (mins.toFloat()/60f) * 100f) + "  Y: " + waitMins)
                    graphEntries.add(Entry(hour.toFloat() * 100f + (mins.toFloat()/60f) * 100f, waitMins.toFloat()))
                }
            }
        }
        if (graphEntries.size != 0) {
            var dataSet = LineDataSet(graphEntries, label)
            dataSet.setColor(color)
            dataSet.valueTextColor = textColor
            return dataSet
        }
        return null
    }

    fun initHistoricChart(rideDPs: AppSyncTest.DisRideDPs): Boolean {
        var historicDataSet = getChartDps(rideDPs.rideDPs, "Wait Times", Color.rgb(200, 200, 200), Color.WHITE)
        if (historicDataSet != null) {
            var xAxisFormatter = HourAxisTimeValueFormatter()
            waitTimeChart.xAxis.setValueFormatter(xAxisFormatter)
            var waitTimeMarkerView = WaitTimeMarkerView(getContext()!!.applicationContext, R.layout.wait_time_marker)
            waitTimeChart.marker = waitTimeMarkerView
            waitTimeChart.axisLeft.axisMinimum = 0f
            waitTimeChart.axisLeft.textColor = Color.WHITE
            waitTimeChart.axisRight.textColor = Color.WHITE
            waitTimeChart.description.text = ""
            waitTimeChart.xAxis.textColor = Color.WHITE
            waitTimeChart.legend.textColor = Color.WHITE
            waitTimeChart.data = LineData(historicDataSet)
            waitTimeChart.invalidate()
            progressBar.visibility = View.GONE
            waitTimeChart.visibility = View.VISIBLE
            return true
        }
        return false
    }

    fun initMultiChart(historicalDPs: List<DisRideDP>, predictedDPs: List<DisRideDP>): Boolean {
        Log.d("STATE", "RIDE DPS not null")
        var historicDataset = getChartDps(historicalDPs, "Wait Times", Color.rgb(200, 200, 200), Color.WHITE)
        var predictDataset = getChartDps(predictedDPs, "Predict Times", Color.rgb(255, 50, 50), Color.WHITE)
        if (historicDataset != null || predictDataset != null) {
            var xAxisFormatter = HourAxisTimeValueFormatter()
            waitTimeChart.xAxis.setValueFormatter(xAxisFormatter)
            var waitTimeMarkerView = WaitTimeMarkerView(getContext()!!.applicationContext, R.layout.wait_time_marker)
            waitTimeChart.marker = waitTimeMarkerView
            //waitTimeChart.axisLeft.axisMinimum = 0f
            waitTimeChart.axisLeft.textColor = Color.WHITE
            waitTimeChart.axisRight.textColor = Color.WHITE
            waitTimeChart.description.text = ""
            waitTimeChart.xAxis.textColor = Color.WHITE
            waitTimeChart.legend.textColor = Color.WHITE
            var setArr = ArrayList<ILineDataSet>()
            if (historicDataset != null) {
                setArr.add(historicDataset)
            }
            if (predictDataset != null) {
                setArr.add(predictDataset)
            }
            waitTimeChart.xAxis.axisMinimum = 9f * 100f
            //setArr.add(predictResult.first)
            waitTimeChart.data = LineData(setArr)
            waitTimeChart.invalidate()
            progressBar.visibility = View.GONE
            waitTimeChart.visibility = View.VISIBLE
            return true
        }
        return false
    }

    fun updateRide(ride: CRInfo) {
        if (!infoSet) {
            var picUrl = ride.picURL
            if (picUrl != null) {
                async(Dispatchers.IO) {
                    picUrl = picUrl?.substring(0, picUrl?.length!! - 4) + "-2" + picUrl?.substring(picUrl?.length!! - 4)
                    Log.d("STATE", "PICURL: " + picUrl)
                    cfm.download(picUrl.toString(), object : CloudFileListener() {
                        override fun onStateChanged(id: Int, state: TransferState?) {}

                        override fun onError(id: Int, ex: Exception?) {}

                        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}

                        override fun onComplete(id: Int, file: File) {
                            Log.d("STATE", "Ride download complete")
                            async(Dispatchers.Main) {
                                rideImg.setImageURI(Uri.fromFile(file))
                            }
                        }
                    })
                }
            }
            Log.d("STATE", "HERE HERE")
            rideNameText.text = ride.name
            infoSet = true
        }
        updateTimes(ride)
    }

    fun updateTimes(ride: CRInfo) {
        if (ride.waitTime != null) {
            rideWaitText.text = ride.waitTime.toString()
            minuteWaitLabel.text = "Minute Wait"
        } else {
            rideWaitText.text = ride.status
            minuteWaitLabel.text = ""
        }
        var fpTimeStr = ride.fpTime
        if (fpTimeStr != null) {
            var date = fpParseFormat.parse(fpTimeStr)
            var cal = GregorianCalendar.getInstance()
            cal.time = date
            var dispStr1 = dateDispFormat.format(date)
            cal.add(Calendar.HOUR, 1)
            var dispStr2 = dateDispFormat.format(cal.time)
            rideFPLabel.text = "FastPasses For"
            rideFPText.text = dispStr1 + " to " + dispStr2
            rideFPSubLabel.text = "Are Being Distributed"
        } else {
            rideFPLabel.text = "FastPasses Are"
            rideFPText.text = "Not Available"
            rideFPSubLabel.text = ""
        }
        var wr = ride.waitRating
        if (wr != null) {
            rideRatingText.text = ratingFormat.format(wr)
            rideRatingSubLabel.text = "Wait Rating"
        } else {
            rideRatingText.text = "No Rating"
            rideRatingSubLabel.text = ""
        }
        lastUpdated = ride.lastChangeTime
        displayLastUpdated.invoke()
        if (lastUpdateRefreshHandler == null) {
            lastUpdateRefreshHandler = Handler()
            lastUpdateRefreshHandler?.postDelayed(displayLastUpdated, 60 * 1000)
        }
        waitTime = ride.waitTime
        fastPassTime = ride.fpTime
        var rideDPs = this@RideFragment.rideDPs
        if (rideDPs != null) {
            initMultiChart(rideDPs)
        }
    }

    fun initMultiChart(rideDPs: AppSyncTest.DisRideDPs) {
        if (rideDPs.rideDPs.size > 0 && waitTime == rideDPs.rideDPs.last().waitTime()) {
            rideDPs.rideDPs.removeAt(rideDPs.rideDPs.lastIndex)
        }
        if (waitTime != null) {
            rideDPs!!.rideDPs.add(DisRideDP("__DisRideDP", waitTime, fastPassTime, dateParser.format(Date())))
        }
        if (rideDPs == null || !initMultiChart(rideDPs.rideDPs, rideDPs.predictedDPs)) {
            noDataText.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }


    override fun onResume() {
        super.onResume()
        async(Dispatchers.Main) {
            rideManager.getRideDPs(rideID, object: AppSyncTest.GetRideDPsCallback {
                override fun onResponse(rideDPs: AppSyncTest.DisRideDPs?) {
                    GlobalScope.async(Dispatchers.Main) {
                        if (rideDPs != null) {
                            this@RideFragment.rideDPs = rideDPs
                            initMultiChart(rideDPs!!)
                        }
                    }
                }

                override fun onError(ec: Int?, msg: String?) {

                }
            })
            rideManager.getRide(rideID, object: RideManager.GetRideCB {
                override fun onUpdate(ride: CRInfo) {
                    updateRide(ride)
                }

                override fun onFinalUpdate(ride: CRInfo) {
                    //updateRide(ride)
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        lastUpdateRefreshHandler?.removeCallbacks(displayLastUpdated)
        lastUpdateRefreshHandler = null
    }

    companion object {
        var RIDEID_PARAM = "objKeys"
        fun GetInstance(rideID: String): RideFragment {
            val fragment = RideFragment()
            val args = Bundle()
            args.putString(RIDEID_PARAM, rideID)
            fragment.arguments = args
            return fragment
        }
    }
}
