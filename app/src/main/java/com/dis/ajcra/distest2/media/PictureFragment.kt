package com.dis.ajcra.distest2.media

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.entity.EntityBarFragment
import com.dis.ajcra.distest2.entity.EntityBarListener
import com.dis.ajcra.distest2.login.CognitoManager
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import java.util.*

interface PictureListener {
    fun onDelete()
}

class PictureFragment : Fragment() {
    private lateinit var cfm: CloudFileManager
    private var entityBar: EntityBarFragment? = null
    private var key: String? = null
    private var photoView: PhotoView? = null
    private var listener: PictureListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            key = arguments!!.getString(ARG_KEY)
        }
    }

    fun setListener(listener: PictureListener) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_picture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.photoView = view!!.findViewById(R.id.picture_photoview)
        entityBar = EntityBarFragment()
        childFragmentManager.beginTransaction().replace(R.id.picture_entitybarholder, entityBar).commit()
        entityBar!!.setListener(object : EntityBarListener {
            override fun onDelete() {
                listener!!.onDelete()
            }

            override fun onGetObjKeys(): ArrayList<String> {
                var objKeys = ArrayList<String>()
                objKeys.add(key as String)
                return objKeys
            }
        })
        var cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        cfm = CloudFileManager(cognitoManager, this.context!!.applicationContext)
        if (key != null) {
            if (!linkToUpload()) {
                download()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //photoView.setImageResource(R.drawable.ic_email_black_24dp)
        photoView?.destroyDrawingCache()
        photoView = null
        Log.d("STATE", "Picture view destroyed")
    }

    fun linkToUpload(): Boolean {
        var observer = cfm.getObservers(TransferType.UPLOAD)[key.toString()]
        if (observer != null) {
            if (observer.addListener(object: CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {}

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}

                override fun onStateChanged(id: Int, state: TransferState?) {}

                override fun onComplete(id: Int, file: File) {}
            })) {
                var bmp = BitmapFactory.decodeFile(observer.file.absolutePath)
                photoView?.setImageBitmap(bmp)
                return true
            }
        }
        return false
    }

    fun download() {
        async {
            cfm.download(key.toString(), object : CloudFileListener() {
                override fun onError(id: Int, ex: Exception?) {}

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}

                override fun onStateChanged(id: Int, state: TransferState?) {}

                override fun onComplete(id: Int, file: File) {
                    async {
                        var bmp = BitmapFactory.decodeFile(file.absolutePath)
                        async(UI) {
                            photoView?.setImageBitmap(bmp)
                        }
                    }
                }
            })
        }
    }

    companion object {
        private val ARG_KEY = "key"

        fun newInstance(key: String): PictureFragment {
            val fragment = PictureFragment()
            val args = Bundle()
            args.putString(ARG_KEY, key)
            fragment.arguments = args
            return fragment
        }
    }
}
