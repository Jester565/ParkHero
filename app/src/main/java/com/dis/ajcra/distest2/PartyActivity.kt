package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.dis.ajcra.distest2.fastpass.FastPassManagerFragment

class PartyActivity : AppCompatActivity() {
    private lateinit var fastPassFragment: FastPassManagerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_party)

        fastPassFragment = FastPassManagerFragment()
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.partyactivity_fastpassLayout, fastPassFragment).commit()
    }
}
