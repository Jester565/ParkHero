package com.dis.ajcra.distest2

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Contacts
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import java.io.File

interface GalleryFragmentListener {
    fun onRecyclerViewCreated(recyclerView: RecyclerView)

    fun onDragChange(dragging: Boolean)
}

interface GridItemTouchHelperAdapter {
    fun onItemMove(sourcePos: Int, targetPos: Int)
}

class GridItemTouchHelperCallback: ItemTouchHelper.Callback {
    private var adapter: GridItemTouchHelperAdapter
    var canDrag: Boolean = false
    constructor(adapter: GridItemTouchHelperAdapter) {
        this.adapter = adapter
    }
    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int {
        if (!canDrag) {
            return makeMovementFlags(0, 0)
        } else {
            var dragFlags = (ItemTouchHelper.UP or ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT)
            return makeMovementFlags(dragFlags, 0)
        }
    }

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean {
        Log.d("STATE", "Over: " + target?.itemId)
        /*
        if (viewHolder!!.itemViewType != target!!.itemViewType) {
            return false
        }
        */
        return true
        //adapter.onItemMove(viewHolder!!.adapterPosition, target!!.adapterPosition)
        //return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {

    }
}

class GalleryFragment : Fragment() {
    private lateinit var cfm: CloudFileManager
    private lateinit var pictures: ArrayList<String>
    lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryFragmentAdapter
    private var helperCB: GridItemTouchHelperCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        var cfm = CloudFileManager.GetInstance(cognitoManager.credentialsProvider, this.context.applicationContext)

        pictures = ArrayList<String>()
        adapter = GalleryFragmentAdapter(cfm, pictures)
        var rootView = inflater!!.inflate(R.layout.fragment_gallery, container, false)
        recyclerView = rootView.findViewById(R.id.gallery_recycler)
        recyclerView.layoutManager = GridLayoutManager(this.context, 3)
        recyclerView.adapter = adapter
        recyclerView.setItemViewCacheSize(50)
        recyclerView.setDrawingCacheEnabled(true)
        helperCB = GridItemTouchHelperCallback(adapter)
        var helper = ItemTouchHelper(helperCB)
        helper.attachToRecyclerView(recyclerView)
        var gfl = parentFragment as GalleryFragmentListener
        gfl.onRecyclerViewCreated(recyclerView)

        async(UI) {
            Log.d("STATE", "Running ui")
            var job = async {
                Log.d("STATE", "Running job")
                cfm.listObjects(cognitoManager.federatedID + "/pictures")
            }
            var objs = job.await()
            Log.d("STATE", "Aquired objects")
            if (objs != null) {
                for (obj in objs) {
                    for (i in 1..10) {
                        pictures.add(obj.key)
                        adapter.notifyItemInserted(pictures.size - 1)
                    }
                }
            }
        }
        return rootView
    }

    fun setDrag(mode: Boolean) {
        helperCB?.canDrag = mode
    }

    //should contain click listeners, can have different ViewHolders
    class GalleryFragmentAdapter: RecyclerView.Adapter<GalleryFragmentAdapter.ViewHolder>, GridItemTouchHelperAdapter {
        override fun onItemMove(sourcePos: Int, targetPos: Int) {
            Log.d("STATE", "Item moved")
        }

        private var cfm: CloudFileManager
        private var dataset: ArrayList<String>

        constructor(cfm: CloudFileManager, dataset: ArrayList<String>) {
            this.cfm = cfm
            this.dataset = dataset
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_picture, parent, false)
            var viewHolder = ViewHolder(view)
            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            async {
                cfm.download(dataset[position], object: CloudFileListener() {
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
                            while (options.outWidth/imgScale > 400) {
                                imgScale *= 2
                            }
                            options.inJustDecodeBounds = false
                            options.inSampleSize = imgScale
                            var bmap = BitmapFactory.decodeFile(file.absolutePath, options)
                            async(UI) {
                                holder?.imgView?.setImageBitmap(bmap)
                            }
                        }
                    }
                })
            }
        }

        override fun getItemCount(): Int {
            return dataset.size
        }

        class ViewHolder: RecyclerView.ViewHolder {
            var imgView: ImageView
            constructor(itemView: View)
                :super(itemView)
            {
                imgView = itemView.findViewById(R.id.row_imgview)
            }
        }
    }
}
