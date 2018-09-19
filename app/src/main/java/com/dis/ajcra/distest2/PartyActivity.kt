package com.dis.ajcra.distest2

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.dis.ajcra.distest2.fastpass.FastPassManagerFragment
import com.dis.ajcra.distest2.prof.PartyFragment

class PartyActivity : AppCompatActivity() {
    private lateinit var partyFragment: PartyFragment
    private lateinit var fastPassFragment: FastPassManagerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_party)

        partyFragment = PartyFragment.GetInstance()
        var partyTransaction = supportFragmentManager.beginTransaction()
        partyTransaction.replace(R.id.partyactivity_partyLayout, partyFragment).commit()
        fastPassFragment = FastPassManagerFragment()
        var fastPassTransaction = supportFragmentManager.beginTransaction()
        fastPassTransaction.replace(R.id.partyactivity_fastpassLayout, fastPassFragment).commit()
    }
}
