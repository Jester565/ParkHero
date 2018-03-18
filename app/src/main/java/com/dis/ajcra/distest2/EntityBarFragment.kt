package com.dis.ajcra.distest2

import android.content.Intent
import android.graphics.Color
import android.media.Image
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.dis.ajcra.distest2.*
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.util.AnimationUtils.Crossfade
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.lang.Exception

interface EntityBarListener {
    fun onGetObjKeys(): ArrayList<String>
    fun onDelete()
}

class EntityBarFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var sendButton: ImageButton
    private lateinit var infoButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var cfm: CloudFileManager

    private var listener: EntityBarListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, this.context.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_entity_bar, container, false)
        return rootView
    }

    fun setListener(listener: EntityBarListener) {
        Log.d("STATE", "setListner called " + listener)
        this@EntityBarFragment.listener = listener
    }

    override fun onViewCreated(rootView: View?, savedInstanceState: Bundle?) {
        if (rootView != null) {
            sendButton = rootView.findViewById(R.id.entitybar_send)
            infoButton = rootView.findViewById(R.id.entitybar_info)
            deleteButton = rootView.findViewById(R.id.entitybar_delete)

            sendButton.setOnClickListener {
                var esf = EntitySendFragment.GetInstance(this.listener!!.onGetObjKeys())
                esf.show(childFragmentManager, "Entity Send")
            }
            deleteButton.setOnClickListener {
                async {
                    this@EntityBarFragment.listener!!.onGetObjKeys().forEach { it ->
                        cfm.delete(it)
                    }
                    async(UI) {
                        this@EntityBarFragment.listener!!.onDelete()
                    }
                }
            }
        }
    }

}