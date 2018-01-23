package com.dis.ajcra.distest2.login

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.dis.ajcra.distest2.*
import com.dis.ajcra.distest2.media.CloudFileListener
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.distest2.prof.ProfileManager
import com.dis.ajcra.distest2.util.AnimationUtils.Crossfade
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import java.lang.Exception

class FriendFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager

    private lateinit var searchField: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FriendFragmentAdapter
    private var dataset: ArrayList<Profile> = ArrayList<Profile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_friend, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        profileManager = ProfileManager(cognitoManager)
        cfm = CloudFileManager.GetInstance(cognitoManager.credentialsProvider, context.applicationContext)
        adapter = FriendFragmentAdapter(cfm, dataset)
        return rootView
    }

    override fun onViewCreated(rootView: View?, savedInstanceState: Bundle?) {
        if (rootView != null) {
            searchField = rootView.findViewById(R.id.friend_searchfield)
            recyclerView = rootView.findViewById(R.id.friend_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true

            searchField.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(p0: Editable?) {

                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(inStr: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    async(UI) {
                        var profiles = profileManager.getUsers(inStr.toString()).await()
                        var i = 0
                        var j = 0
                        while (j < profiles.size) {
                            while (i < dataset.size) {
                                var n1 = dataset[i].getName().await()
                                var n2 = profiles[j].getName().await()
                                if (n1.compareTo(n2, true) < 0) {
                                    dataset.removeAt(i)
                                    adapter.notifyItemRemoved(i)
                                } else {
                                    break
                                }
                            }
                            if (i >= dataset.size || dataset[i].id != profiles[j].id) {
                                dataset.add(i, profiles[j])
                                adapter.notifyItemInserted(i)
                            }
                            i++
                            j++
                        }
                        while (i < dataset.size) {
                            dataset.removeAt(i)
                            adapter.notifyItemRemoved(i)
                            i++
                        }
                    }
                }
            })
            //populate with friends
        }
    }


    //should contain click listeners, can have different ViewHolders
    class FriendFragmentAdapter: RecyclerView.Adapter<FriendFragmentAdapter.ViewHolder> {
        private var cfm: CloudFileManager
        private var dataset: ArrayList<Profile>

        constructor(cfm: CloudFileManager, dataset: ArrayList<Profile>) {
            this.cfm = cfm
            this.dataset = dataset
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_profile, parent, false)
            var viewHolder = ViewHolder(view)
            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            holder!!.rootView.setOnClickListener {
                var profile = dataset[holder!!.adapterPosition]
                var intent = Intent(holder!!.ctx, ProfileActivity::class.java)
                intent.putExtra("id", profile.id)
                holder!!.ctx.startActivity(intent)
            }
            async(UI) {
                holder!!.profNameView.text = dataset[position].getName().await()
                Log.d("STATE", "Name set: " + holder!!.profNameView.text)
            }
            async {
                var profilePicUrl = dataset[position].getProfilePicUrl().await()
                if (profilePicUrl == null) {
                    profilePicUrl = "profileImgs/blank-profile-picture-973460_640.png"
                }
                cfm.download(profilePicUrl, object : CloudFileListener() {
                    override fun onError(id: Int, ex: Exception?) {

                    }

                    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                        async(UI) {

                        }
                    }

                    override fun onStateChanged(id: Int, state: TransferState?) {
                        async(UI) {

                        }
                    }

                    override fun onComplete(id: Int, file: File) {
                        async {
                            var options = BitmapFactory.Options()
                            options.inJustDecodeBounds = true
                            BitmapFactory.decodeFile(file.absolutePath, options)
                            var imgScale = 1
                            while (options.outWidth / imgScale > 400) {
                                imgScale *= 2
                            }
                            options.inJustDecodeBounds = false
                            options.inSampleSize = imgScale
                            var bmap = BitmapFactory.decodeFile(file.absolutePath, options)
                            async(UI) {
                                holder!!.profImgView!!.setImageBitmap(bmap)
                            }
                        }
                    }
                })
            }
        }

        override fun getItemCount(): Int {
            return dataset.size
        }

        class ViewHolder : RecyclerView.ViewHolder {
            var profNameView: TextView
            var profImgView: ImageView
            var rootView: View
            var ctx: Context

            constructor(itemView: View)
                    : super(itemView) {
                ctx = itemView.context
                rootView = itemView
                profNameView = itemView.findViewById(R.id.rowprof_profname)
                profImgView = itemView.findViewById(R.id.rowprof_profimg)
            }
        }
    }
}