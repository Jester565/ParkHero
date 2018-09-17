package com.dis.ajcra.distest2.pass

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.prof.ProfileManager
import com.google.gson.JsonParser
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async

class RenameProfileFragment : DialogFragment() {
    companion object {
        fun GetInstance(name: String): RenameProfileFragment {
            val fragment = RenameProfileFragment()
            fragment.arguments = Bundle()
            fragment.arguments!!.putString("name", name)
            return fragment
        }
    }

    private lateinit var nameEditText: EditText
    private lateinit var checkButton: FloatingActionButton
    private lateinit var changeProgressBar: ProgressBar
    private var onRenameCallback: ((String) -> Unit)? = null

    private lateinit var profileManager: ProfileManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileManager = ProfileManager.GetInstance(context!!.applicationContext)
    }

    fun setOnRenameCallback(cb: ((String) -> Unit)?) {
        this.onRenameCallback = cb
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_rename_profile, container, false)
        nameEditText = rootView.findViewById(R.id.renameprofile_nameEdit)
        checkButton = rootView.findViewById(R.id.renameprofile_checkButton)
        changeProgressBar = rootView.findViewById(R.id.renameprofile_progress)

        nameEditText.setText(arguments!!.getString("name")!!, TextView.BufferType.EDITABLE)

        nameEditText.setOnEditorActionListener { textView, actionID, event ->
            var done = false
            if (actionID == EditorInfo.IME_ACTION_DONE) {
                rename(nameEditText.text.toString())
                done = true
            }
            done
        }

        checkButton.setOnClickListener {
            rename(nameEditText.text.toString())
        }
        return rootView
    }

    fun rename(name: String) {
        async(UI) {
            nameEditText.setFocusable(false)
            checkButton.visibility = View.GONE
            changeProgressBar.visibility = View.VISIBLE
            var myProfile = profileManager.getMyProfile().await()!!
            try {
                myProfile.rename(nameEditText.text.toString()).await()
                Toast.makeText(context, "Rename Successful!", Toast.LENGTH_SHORT).show()
                onRenameCallback?.invoke(nameEditText.text.toString())
                dismiss()
            } catch (ex: ApiClientException) {
                var jObj = JsonParser().parse(ex.errorMessage).getAsJsonObject()
                var message: String = jObj.get("message").asString
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            nameEditText.setFocusable(true)
            checkButton.visibility = View.VISIBLE
            changeProgressBar.visibility = View.GONE
        }
    }
}