package com.dis.ajcra.distest2.pass

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.camera.CameraActivity
import com.dis.ajcra.distest2.login.CognitoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AddPassFragment : DialogFragment() {
    companion object {
        fun GetInstance(): AddPassFragment {
            val fragment = AddPassFragment()
            return fragment
        }
    }

    private lateinit var textLayout: RelativeLayout
    private lateinit var passIDEditText: EditText
    private lateinit var addButton: FloatingActionButton
    private lateinit var cameraButton: FloatingActionButton
    private lateinit var addProgressBar: ProgressBar

    private lateinit var passManager: PassManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passManager = PassManager.GetInstance(CognitoManager.GetInstance(context!!.applicationContext), context!!.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_add_pass, container, false)
        passIDEditText = rootView.findViewById(R.id.addpass_passEdit)
        addButton = rootView.findViewById(R.id.addpass_passaddButton)
        cameraButton = rootView.findViewById(R.id.addpass_cameraButton)
        addProgressBar = rootView.findViewById(R.id.addpass_progress)

        passIDEditText.setOnEditorActionListener { textView, actionID, event ->
            var done = false
            if (actionID == EditorInfo.IME_ACTION_DONE) {
                addPass(passIDEditText.text.toString())
                done = true
            }
            done
        }

        addButton.setOnClickListener {
            addPass(passIDEditText.text.toString())
        }

        cameraButton.setOnClickListener {
            Toast.makeText(context, "Take a picture of the barcode on your pass", Toast.LENGTH_LONG).show()
            var intent = Intent(context, CameraActivity::class.java)
            intent.putExtra("videoEnabled", false)
            intent.putExtra("galleryEnabled", false)
            startActivity(intent)
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    fun addPass(passID: String) {
        GlobalScope.async(Dispatchers.Main) {
            addButton.visibility = View.GONE
            addProgressBar.visibility = View.VISIBLE
            var pass = passManager.addPass(passID)
            if (pass != null) {
                Toast.makeText(context, "Pass Added!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Invalid Pass...", Toast.LENGTH_LONG).show()
            }
            addButton.visibility = View.VISIBLE
            addProgressBar.visibility = View.GONE
        }
    }
}