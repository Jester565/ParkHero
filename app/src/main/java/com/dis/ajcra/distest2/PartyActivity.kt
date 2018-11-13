package com.dis.ajcra.distest2

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.dis.ajcra.distest2.fastpass.FastPassManagerFragment
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.pass.AddPassFragment
import com.dis.ajcra.distest2.pass.PassFragment
import com.dis.ajcra.distest2.pass.PassManager
import com.dis.ajcra.distest2.prof.PartyFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class PartyActivity : FragmentActivity() {
    private lateinit var passFragment: PassFragment
    private lateinit var partyFragment: PartyFragment
    private lateinit var fastPassFragment: FastPassManagerFragment
    private lateinit var passManager: PassManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_party)

        initPassManager()
        partyFragment = PartyFragment.GetInstance()
        var partyTransaction = supportFragmentManager.beginTransaction()
        partyTransaction.replace(R.id.partyactivity_partyLayout, partyFragment).commit()
        fastPassFragment = FastPassManagerFragment()
        var fastPassTransaction = supportFragmentManager.beginTransaction()
        fastPassTransaction.replace(R.id.partyactivity_fastpassLayout, fastPassFragment).commit()
    }

    fun initPassManager() {
        passManager = PassManager.GetInstance(CognitoManager.GetInstance(applicationContext), applicationContext)

        passFragment = PassFragment.GetInstance()
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
                                Toast.makeText(this@PartyActivity, "Pass Deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@PartyActivity, "Failed to delete pass", Toast.LENGTH_LONG).show()
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
        passTransaction.replace(R.id.partyactivity_passLayout, passFragment).commit()
    }
}
