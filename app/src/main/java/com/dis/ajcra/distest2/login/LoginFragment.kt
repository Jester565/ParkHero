package com.dis.ajcra.distest2.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.dis.ajcra.distest2.MainActivity
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.util.AnimationUtils.Crossfade
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
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
    private lateinit var googleButton: SignInButton
    private lateinit var gso: GoogleSignInOptions
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments!!.containsKey("pwd")) {
            pwd = arguments!!.getString(PWD_PARAM)
        }
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("484305592931-pcg7s9kmq920p2csgmmc4ga0suo98vuh.apps.googleusercontent.com")
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(activity!!.application, gso)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_login, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        return rootView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
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
            googleButton = rootView.findViewById(R.id.login_gbutton)
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

        googleButton.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        if (username != null && pwd != null) {
            login()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val completedTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = completedTask.getResult(ApiException::class.java)
                var idToken = account.idToken
                if (idToken != null) {
                    cognitoManager.addLogin("accounts.google.com", idToken)
                    Log.d("STATE", "Login Complete")
                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: ApiException) {
                Log.w("STATE", "signInResult:failed code=" + e.statusCode + ":" + e.message)
            }

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
        private val RC_SIGN_IN = 222

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