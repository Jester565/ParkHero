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
import android.widget.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import com.dis.ajcra.distest2.*
import com.dis.ajcra.distest2.AnimationUtils.Crossfade
import org.w3c.dom.Text
import java.lang.Exception

class VerifyFragment : Fragment() {
    private lateinit var progressBar: ProgressBar

    private lateinit var cognitoManager: CognitoManager
    private var pwd: String? = null
    private var deliveryMethod: String? = null
    private var deliveryDest: String? = null

    private lateinit var verifyLayout: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var codeField: EditText
    private lateinit var submitButton: Button
    private lateinit var instructionText: TextView
    private lateinit var resendButton: Button
    private lateinit var msgText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pwd = arguments.getString(PWD_PARAM)
        deliveryMethod = arguments.getString(DELVMETH_PARAM)
        deliveryDest = arguments.getString(DELVDEST_PARAM)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_verify, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        return rootView
    }

    override fun onViewCreated(rootView: View?, savedInstanceState: Bundle?) {
        if (rootView != null) {
            verifyLayout = rootView.findViewById(R.id.verify_linearLayout)
            titleText = rootView.findViewById(R.id.verify_titleText)
            codeField = rootView.findViewById(R.id.verify_codeField)
            submitButton = rootView.findViewById(R.id.verify_submitCodeButton)
            instructionText = rootView.findViewById(R.id.verify_submitCodeText)
        }
        initInstructions()
        codeField.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                submitButton.isEnabled = s!!.length >= 4
                submitButton.setBackgroundResource(android.R.drawable.btn_default)
                submitButton.text = "Submit Code"
            }
        })

        submitButton.setOnClickListener { v ->
            v.isEnabled = false
            msgText.text = ""
            Crossfade(progressBar, verifyLayout)
            cognitoManager.validateUser(codeField.text.toString(), object: GenericHandler {
                override fun onSuccess() {
                    /*
                    val intent = Intent(this@VerifyFragment, LoginActivity::class.java)
                    if (pwd != null) {
                        intent.putExtra("pwd", pwd)
                    }
                    startActivity(intent)
                    */
                }

                override fun onFailure(exception: Exception?) {
                    submitButton.setBackgroundColor(Color.RED)
                    submitButton.text = "ERROR"
                    msgText.text = exception!!.message
                    Crossfade(verifyLayout, progressBar)
                }
            })
        }

        resendButton.setOnClickListener { v ->
            v.isEnabled = false
            msgText.text = ""
            resendButton.text = "Resend"
            resendButton.setBackgroundResource(android.R.drawable.btn_default)
            cognitoManager.resendCode(object: CognitoManager.ResendCodeHandler {
                override fun onSuccess(delvMeth: String?, delvDest: String?) {
                    resendButton.text = "Done!"
                    resendButton.setBackgroundColor(Color.GREEN)
                    resendButton.isEnabled = false
                }

                override fun onFailure(ex: Exception?) {
                    resendButton.setBackgroundColor(Color.RED)
                    resendButton.text = "ERROR"
                }
            })
        }
    }

    private fun initInstructions() {
        titleText.text = "Verify " + deliveryMethod
        instructionText.text = "We sent a $deliveryMethod to $deliveryDest. Enter the code above so we can finish setting up your account."
    }

    companion object {
        private val PWD_PARAM = "pwd"
        private val DELVMETH_PARAM = "delvMeth"
        private val DELVDEST_PARAM = "delvDest"

        fun newInstance(pwd: String, delvmethod: String, delvDest: String): VerifyFragment {
            val fragment = VerifyFragment()
            val args = Bundle()
            args.putString(PWD_PARAM, pwd)
            args.putString(DELVMETH_PARAM, delvmethod)
            args.putString(DELVDEST_PARAM, delvDest)
            fragment.arguments = args
            return fragment
        }
    }
}