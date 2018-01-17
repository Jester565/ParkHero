import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.dis.ajcra.distest2.*
import com.dis.ajcra.distest2.AnimationUtils.Crossfade
import org.w3c.dom.Text
import java.lang.Exception

class RegisterFragment : Fragment() {
    private lateinit var registerProgressBar: ProgressBar

    private lateinit var cognitoManager: CognitoManager

    private lateinit var registerLayout: View
    private lateinit var registerButton: Button
    private lateinit var usernameField: EditText
    private lateinit var emailField: EditText
    private lateinit var pwdField: EditText
    private lateinit var msgText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_register, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        return rootView
    }

    override fun onViewCreated(rootView: View?, savedInstanceState: Bundle?) {
        if (rootView != null) {
            registerProgressBar = rootView.findViewById(R.id.register_progressBar)

            registerLayout = rootView.findViewById(R.id.register_linearLayout)
            usernameField = rootView.findViewById(R.id.register_usernameField)
            emailField = rootView.findViewById(R.id.register_emailField)
            pwdField = rootView.findViewById(R.id.register_pwdField)
            registerButton = rootView.findViewById(R.id.register_submitButton)
            msgText = rootView.findViewById(R.id.register_msgText)

            var textChangeListener = object: TextWatcher {
                override fun afterTextChanged(p0: Editable?) {

                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    registerButton.text = "CREATE ACCOUNT"
                    registerButton.setBackgroundResource(android.R.drawable.btn_default)
                    registerButton.isEnabled = (usernameField.length() > 0 && emailField.length() > 0 && pwdField.length()> 0)
                }
            }
            usernameField.addTextChangedListener(textChangeListener)
            emailField.addTextChangedListener(textChangeListener)
            pwdField.addTextChangedListener(textChangeListener)

            registerButton.setOnClickListener {  v ->
                v.isEnabled = false
                AnimationUtils.HideKeyboard(this.activity)
                msgText.text = ""
                AnimationUtils.Crossfade(registerProgressBar, registerLayout)
                cognitoManager.registerUser(usernameField.text.toString(), emailField.text.toString(), pwdField.text.toString(),
                        object: CognitoManager.RegisterUserHandler {
                            override fun onSuccess() {
                                /*
                                val intent = Intent(this@RegisterFragment, LoginActivity::class.java)
                                intent.putExtra("pwd", pwdField.text.toString())
                                startActivity(intent)
                                */
                            }

                            override fun onVerifyRequired(deliveryMethod: String?, deliveryDest: String?) {
                                /*
                                val intent = Intent(this@RegisterFragment, VerifyActivity::class.java)
                                intent.putExtra("pwd", pwdField.text.toString())
                                startActivity(intent)
                                */
                            }

                            override fun onFailure(ex: Exception?) {
                                registerButton.text = "ERROR"
                                registerButton.setBackgroundColor(Color.RED)
                                msgText.text = ex!!.message
                                Crossfade(registerLayout, registerProgressBar)
                            }
                        })
            }
        }
    }
}