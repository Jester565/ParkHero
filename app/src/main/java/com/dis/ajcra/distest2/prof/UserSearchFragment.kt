package com.dis.ajcra.distest2.login

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.distest2.prof.ProfileManager
import com.dis.ajcra.distest2.prof.ProfileRecyclerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class UserSearchFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var profileManager: ProfileManager
    private lateinit var cfm: CloudFileManager

    private lateinit var searchField: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfileRecyclerAdapter
    private var dataset = ArrayList<Profile>()

    private lateinit var subLoginToken: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_user_search, container, false)
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        profileManager = ProfileManager(cognitoManager, this.context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
        adapter = ProfileRecyclerAdapter(cfm, dataset)
        return rootView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        if (rootView != null) {
            searchField = rootView.findViewById(R.id.usersearch_searchfield)
            recyclerView = rootView.findViewById(R.id.usersearch_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true
            //populate with friends
        }
    }

    override fun onResume() {
        super.onResume()
        subLoginToken = cognitoManager.subscribeToLogin { ex->
            if (ex == null) {
                searchField.addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {}

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun onTextChanged(inStr: CharSequence?, start: Int, before: Int, count: Int) {
                        GlobalScope.async(Dispatchers.Main) {
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
                            }
                        }
                    }
                })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cognitoManager.unsubscribeFromLogin(subLoginToken)
    }
}