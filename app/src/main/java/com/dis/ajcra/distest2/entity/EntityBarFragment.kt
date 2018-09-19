package com.dis.ajcra.distest2.entity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.ProfileManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async

interface EntityBarListener {
    fun onGetObjKeys(): ArrayList<String>
    fun onDelete()
}

class EntityBarFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var sendButton: ImageButton
    private lateinit var infoButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var cfm: CloudFileManager

    private var listener: EntityBarListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        profileManager = ProfileManager.GetInstance(this.context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, this.context!!.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_entity_bar, container, false)
        return rootView
    }

    fun setListener(listener: EntityBarListener) {
        Log.d("STATE", "setListner called " + listener)
        this@EntityBarFragment.listener = listener
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        if (rootView != null) {
            sendButton = rootView.findViewById(R.id.entitybar_send)
            infoButton = rootView.findViewById(R.id.entitybar_info)
            deleteButton = rootView.findViewById(R.id.entitybar_delete)

            sendButton.setOnClickListener {
                var esf = SendFragment.GetInstance()
                esf.sendCallback = { profiles ->
                    var objKeys = listener!!.onGetObjKeys()
                    objKeys.forEach { it ->
                        profileManager.sendEntity(it, ArrayList(profiles)).await()
                    }
                    true
                }
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