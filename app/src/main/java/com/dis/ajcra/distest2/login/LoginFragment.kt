package com.dis.ajcra.distest2.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.dis.ajcra.distest2.*
import com.dis.ajcra.distest2.util.AnimationUtils.Crossfade
import java.lang.Exception

class LoginFragment : Fragment() {
    private lateinit var progressBar: ProgressBar

    private lateinit var cognitoManager: CognitoManager
    private var pwd: String? = null
    private lateinit var loginLayout: LinearLayout
    private lateinit var usernameField: EditText
    private lateinit var pwdField: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPwdButton: Button
    private lateinit var registerButton: Button
    private lateinit var msgText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments.containsKey("pwd")) {
            pwd = arguments.getString(PWD_PARAM)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_login, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        return rootView
    }

    override fun onViewCreated(rootView: View?, savedInstanceState: Bundle?) {
        var username = cognitoManager.userID
        if (rootView != null) {
            progressBar = rootView.findViewById(R.id.login_progressBar)

            loginLayout = rootView.findViewById(R.id.login_linearLayout)
            usernameField = rootView.findViewById(R.id.login_usernameField)
            pwdField = rootView.findViewById(R.id.login_pwdField)
            loginButton = rootView.findViewById(R.id.login_loginButton)
            forgotPwdButton = rootView.findViewById(R.id.login_resetPwdButton)
            msgText = rootView.findViewById(R.id.login_msgText)
            registerButton = rootView.findViewById(R.id.login_registerButton)
        }

        if (username != null) {
            usernameField.setText(username)
        }
        if (pwd != null) {
            pwdField.setText(pwd)
        }

        registerButton.setOnClickListener {
            var intent = Intent(context, RegisterActivity::class.java)
            startActivity(intent)
        }

        var textChangeListener = object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                loginButton.isEnabled = (usernameField.length() > 0 && pwdField.length() > 0)
                loginButton.setBackgroundResource(android.R.drawable.btn_default)
                loginButton.text = "Login"
            }
        }

        usernameField.addTextChangedListener(textChangeListener)
        pwdField.addTextChangedListener(textChangeListener)

        loginButton.setOnClickListener {
            login()
        }

        forgotPwdButton.setOnClickListener {
            val intent = Intent(context, ResetPwdActivity::class.java)
            startActivity(intent)
        }
        if (username != null && pwd != null) {
            login()
        }
    }

    private fun login() {
        loginButton.isEnabled = false
        msgText.text = ""
        Crossfade(progressBar, loginLayout)
        cognitoManager.login(usernameField.text.toString(), pwdField.text.toString(), object: CognitoManager.LoginHandler() {
            override fun onSuccess() {
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
            }

            override fun onMFA(continuation: MultiFactorAuthenticationContinuation) {

            }

            override fun onFailure(ex: Exception) {
                msgText.text = ex!!.message
                loginButton.setBackgroundColor(Color.RED)
                loginButton.text = "ERROR"
                Crossfade(loginLayout, progressBar)
            }

            override fun onUnverified(ex: Exception) {
                val intent = Intent(context, VerifyActivity::class.java)
                intent.putExtra("pwd", pwdField.text.toString())
                startActivity(intent)
            }
        })
    }

    companion object {
        private val PWD_PARAM = "pwd"

        fun newInstance(pwd: String? = null): LoginFragment {
            val fragment = LoginFragment()
            val args = Bundle()
            if (pwd != null)
                args.putString(PWD_PARAM, pwd)
            fragment.arguments = args
            return fragment
        }
    }
}