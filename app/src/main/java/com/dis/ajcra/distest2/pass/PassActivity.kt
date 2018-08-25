package com.dis.ajcra.distest2.pass

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.dis.ajcra.distest2.ParkScheduleFragment
import com.dis.ajcra.distest2.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async


class PassActivity : FragmentActivity() {
    private lateinit var scheduleLayout: LinearLayout

    private lateinit var passFragment: PassFragment
    private lateinit var scheduleFragment: ParkScheduleFragment

    private lateinit var passManager: PassManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pass)
        passManager = PassManager.GetInstance(applicationContext)

        scheduleLayout = findViewById(R.id.pass_scheduleLayout)

        passFragment = PassFragment.GetInstance()
        scheduleFragment = ParkScheduleFragment.GetInstance()
        passFragment.setOnLoadCallback {arr ->
            if (arr.size == 0) {
                scheduleLayout.visibility = View.GONE
            } else {
                scheduleLayout.visibility = View.VISIBLE
            }
            false
        }
        passFragment.setPassChangeCallback { pass ->
            if (pass.type() != null) {
                var blockLevel = ParkScheduleFragment.PassTypes.indexOfFirst { type->
                    type == pass.type()
                }
                scheduleFragment.setMaxBlockLevel(blockLevel)
            }
        }
        passFragment.setOnAddCallback {
            var esf = AddPassFragment.GetInstance()
            esf.show(supportFragmentManager, "Entity Send")
        }

        passFragment.setOnRemoveCallback { pass ->
            val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        async(UI) {
                            var removed = passManager.removePass(pass.id()!!)
                            if (removed) {
                                Toast.makeText(this@PassActivity, "Pass Deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@PassActivity, "Failed to delete pass", Toast.LENGTH_LONG).show()
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
        passTransaction.replace(R.id.pass_passLayout, passFragment).commit()
        var scheduleTransaction = supportFragmentManager.beginTransaction()
        scheduleTransaction.replace(R.id.pass_scheduleLayout, scheduleFragment).commit()
    }
}
