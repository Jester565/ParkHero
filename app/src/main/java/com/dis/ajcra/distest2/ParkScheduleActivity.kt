package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

class ParkScheduleActivity : FragmentActivity() {
    private lateinit var passSpinner: Spinner
    private lateinit var parkScheduleFragment: ParkScheduleFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_park_schedule)
        passSpinner = findViewById(R.id.parkschedule_passSpinner)

        parkScheduleFragment = ParkScheduleFragment.GetInstance()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.parkschedule_layout, parkScheduleFragment).commit()

        var passAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, ParkScheduleFragment.PassTypes)
        passAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        passSpinner.adapter = passAdapter
        passSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                parkScheduleFragment.setMaxBlockLevel(position)
            }
        }
    }
}
