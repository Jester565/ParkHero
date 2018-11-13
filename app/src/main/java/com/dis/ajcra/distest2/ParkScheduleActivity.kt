package com.dis.ajcra.distest2

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.pass.AddPassFragment
import com.dis.ajcra.distest2.pass.PassFragment
import com.dis.ajcra.distest2.pass.PassManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class ParkScheduleActivity : FragmentActivity() {
    private lateinit var passManager: PassManager
    private lateinit var passSpinner: Spinner
    private lateinit var passFragment: PassFragment
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

        passManager = PassManager.GetInstance(CognitoManager.GetInstance(applicationContext), applicationContext)
        passFragment = PassFragment.GetInstance()
        passFragment.setPassChangeCallback { pass ->
            if (pass.type() != null) {
                var blockLevel = 0
                if (pass.type() == "socal-annual") {
                    blockLevel = 1
                }
                passSpinner.setSelection(blockLevel)
                parkScheduleFragment.setMaxBlockLevel(blockLevel)
            }
        }
        passFragment.setOnAddCallback {
            var esf = AddPassFragment.GetInstance()
            esf.show(supportFragmentManager, "Add Pass")
        }

        passFragment.setOnRemoveCallback { pass ->
            val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        GlobalScope.async(Dispatchers.Main) {
                            var removed = passManager.removePass(pass.id()!!)
                            if (removed) {
                                Toast.makeText(this@ParkScheduleActivity, "Pass Deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@ParkScheduleActivity, "Failed to delete pass", Toast.LENGTH_LONG).show()
                            }
                            dialog.cancel()
                        }
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                        dialog.cancel()
                    }
                }
            }

            val builder = AlertDialog.Builder(this)
            builder.setMessage("Forget about " + pass.name() + "'s pass?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show()
        }
        var passTransaction = supportFragmentManager.beginTransaction()
        passTransaction.replace(R.id.parkschedule_passLayout, passFragment).commit()
    }
}
