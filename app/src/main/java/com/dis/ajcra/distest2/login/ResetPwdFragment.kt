package com.dis.ajcra.distest2.login

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.util.AnimationUtils.Crossfade
import java.lang.Exception

class ResetPwdFragment : Fragment() {
    private lateinit var progressBar: ProgressBar

    private lateinit var cognitoManager: CognitoManager
    private lateinit var userLayout: LinearLayout
    private lateinit var codeLayout: LinearLayout
    private lateinit var usernameField: EditText
    private lateinit var usernameSubmitButton: Button
    private lateinit var codeField: EditText
    private lateinit var pwdField: EditText
    private lateinit var pwdResetButton: Button
    private lateinit var msgText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_reset_pwd, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        return rootView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        if (rootView != null) {
            progressBar = rootView.findViewById(R.id.reset_progressBar)

            userLayout = rootView.findViewById(R.id.reset_usernameLayout)
            codeLayout = rootView.findViewById(R.id.reset_codeLayout)
            usernameField = rootView.findViewById(R.id.reset_usernameField)
            usernameSubmitButton = rootView.findViewById(R.id.reset_usernameButton)
            codeField = rootView.findViewById(R.id.reset_codeField)
            pwdField = rootView.findViewById(R.id.reset_pwdField)
            pwdResetButton = rootView.findViewById(R.id.reset_resetButton)
            msgText = rootView.findViewById(R.id.reset_msgText)

            var username = cognitoManager.userID
            if (username != null) {
                usernameField.setText(username)
            }

            usernameField.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(p0: Editable?) {

                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    usernameSubmitButton.isEnabled = usernameField.length() > 0
                }
            })

            usernameSubmitButton.setOnClickListener { v ->
                v.isEnabled = false
                msgText.text = ""
                Crossfade(progressBar, userLayout)
                cognitoManager.resetPwd(usernameField.text.toString(), object: CognitoManager.ResetPwdHandler {
                    override fun onSuccess() {
                        var intent = Intent(context, LoginActivity::class.java)
                        intent.putExtra("pwd", pwdField.text.toString())
                        startActivity(intent)
                    }

                    override fun onContinuation(continuation: ForgotPasswordContinuation) {
                        Crossfade(codeLayout, progressBar)
                        pwdResetButton.setOnClickListener { v ->
                            v.isEnabled = false
                            continuation.setVerificationCode(codeField.text.toString())
                            continuation.setPassword(pwdField.text.toString())
                            continuation.continueTask()
                        }
                    }

                    override fun onFailure(ex: Exception) {
                        msgText.text = ex.message
                        if (pwdField.length() == 0) {
                            Crossfade(userLayout, progressBar)
                        } else {
                            Crossfade(codeLayout, progressBar)
                        }
                    }
                })
            }
        }
        var codeTextChangeListener = object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                pwdResetButton.isEnabled = (pwdField.length() > 0 && codeField.length() >= 4)
            }
        }
        pwdField.addTextChangedListener(codeTextChangeListener)
        codeField.addTextChangedListener(codeTextChangeListener)
    }
}