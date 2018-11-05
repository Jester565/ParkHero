package com.dis.ajcra.distest2.media

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.camera.CameraFragment
import com.dis.ajcra.distest2.login.CognitoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

interface GalleryFragmentListener {
    fun onRecyclerViewCreated(recyclerView: RecyclerView)

    fun onDragChange(dragging: Boolean)
}

/*
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
*/

class GalleryFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var cfm: CloudFileManager
    private lateinit var pictures: ArrayList<String>
    lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryFragmentAdapter
    private lateinit var sendButton: ImageButton

    private lateinit var subLoginToken: String

    private var subTransferToken: UUID? = null

    override fun onResume() {
        super.onResume()
        subLoginToken = cognitoManager.subscribeToLogin { ex ->
            GlobalScope.launch(Dispatchers.Main) {
                adapter.notifyItemRangeRemoved(0, pictures.size)
                pictures.clear()
                if (subTransferToken != null) {
                    cfm.removeTransferListener(subTransferToken!!)
                }

                subTransferToken = cfm.addTransferListener({ objKey, transferType ->
                    GlobalScope.launch(Dispatchers.Main) {
                        Log.d("STATE", "TRANSFER CALLED")
                        if (transferType == TransferType.UPLOAD) {
                            pictures.add(0, objKey)
                            adapter.notifyItemInserted(0)
                        }
                    }
                })
                var observerPairs = cfm.getObservers(TransferType.UPLOAD)
                for (observerPair in observerPairs) {
                    pictures.add(observerPair.key)
                    adapter.notifyItemInserted(pictures.size - 1)
                }
                var job = GlobalScope.async(Dispatchers.IO) {
                    Log.d("STATE", "Running job")
                    cfm.listObjects("media/" + cognitoManager.federatedID, true)
                }
                var objs = job.await()
                Log.d("STATE", "Aquired objects")
                if (objs != null) {
                    for (obj in objs) {
                        if (!pictures.contains(obj.key)) {
                            pictures.add(obj.key)
                            adapter.notifyItemInserted(pictures.size - 1)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cognitoManager.unsubscribeFromLogin(subLoginToken)
        if (subTransferToken != null) {
            cfm.removeTransferListener(subTransferToken!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, this.context!!.applicationContext)

        pictures = ArrayList<String>()
        adapter = GalleryFragmentAdapter(cfm, pictures)
        var rootView = inflater!!.inflate(R.layout.fragment_gallery, container, false)
        recyclerView = rootView.findViewById(R.id.gallery_recycler)
        recyclerView.layoutManager = GridLayoutManager(this.context, 3)
        recyclerView.adapter = adapter
        recyclerView.setItemViewCacheSize(50)
        recyclerView.setDrawingCacheEnabled(true)
        var gfl = parentFragment as GalleryFragmentListener
        gfl.onRecyclerViewCreated(recyclerView)
        sendButton = rootView.findViewById(R.id.gallery_sendbtn)
        recyclerView.setOnDragListener(object: View.OnDragListener {
            override fun onDrag(p0: View?, evt: DragEvent?): Boolean {
                if (evt?.action == DragEvent.ACTION_DRAG_STARTED) {
                    sendButton.alpha = 0f
                    sendButton.visibility = View.VISIBLE
                    sendButton.animate().setDuration(CameraFragment.BAR_TRANSITION_MILLIS).alpha(1f).setListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {

                        }
                    })
                }
                return true
            }
        })
        sendButton.setOnDragListener(object: View.OnDragListener {
            override fun onDrag(view: View?, evt: DragEvent?): Boolean {
                Log.d("STATE", "on drag called")
                if (evt != null && view != null) {
                    Log.d("STATE", "od")
                    when (evt.action) {
                        DragEvent.ACTION_DRAG_STARTED -> {
                            Log.d("STATE", "Drag started")
                            view.alpha = 0f
                            view.visibility = View.VISIBLE
                            view.animate().setDuration(CameraFragment.BAR_TRANSITION_MILLIS).alpha(1f).setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {

                                }
                            })
                        }
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            Log.d("STATE", "Drag entered")
                            view.animate().setDuration(100).scaleX(1.5f).setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {

                                }
                            })
                            view.animate().setDuration(100).scaleY(1.5f).setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {

                                }
                            })
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            Log.d("STATE", "Drag exited")
                            view.animate().setDuration(100).scaleX(1f).setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {

                                }
                            })

                            view.animate().setDuration(100).scaleY(1f).setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {

                                }
                            })
                        }
                        DragEvent.ACTION_DROP -> {
                            Log.d("STATE", "Drag drop")
                            view.setBackgroundColor(Color.RED)
                        }
                        DragEvent.ACTION_DRAG_ENDED -> {
                            Log.d("STATE", "Drag ended")
                            view.animate().setDuration(CameraFragment.BAR_TRANSITION_MILLIS).alpha(0f).setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    view.visibility = View.GONE
                                    view.scaleX = 1f
                                    view.scaleY = 1f
                                    view.setBackgroundColor(Color.GRAY)
                                }
                            })
                        }
                    }
                }
                return true
            }
        })
        return rootView
    }

    fun galleryUpdated(objKey: String) {
        /*
        Log.d("STATE", "Gallery updated")
        async(UI) {
            pictures.add(objKey)
            adapter.notifyItemInserted(pictures.size - 1)
        }
        */
    }

    fun setDrag(mode: Boolean) {

    }

    //should contain click listeners, can have different ViewHolders
    class GalleryFragmentAdapter: RecyclerView.Adapter<GalleryFragmentAdapter.ViewHolder> {
        private var cfm: CloudFileManager
        private var dataset: ArrayList<String>

        constructor(cfm: CloudFileManager, dataset: ArrayList<String>) {
            this.cfm = cfm
            this.dataset = dataset
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(parent?.context).inflate(R.layout.row_picture, parent, false)
            var viewHolder = ViewHolder(view)
            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder?.imgView?.tag = position.toString()
            holder?.imgView?.setOnLongClickListener(object: View.OnLongClickListener {
                override fun onLongClick(view: View?): Boolean {
                    if (view != null) {
                        var item = ClipData.Item(view.tag as String)
                        var mimeTypes: Array<String> = Array<String>(1, {ClipDescription.MIMETYPE_TEXT_PLAIN})

                        var dragData = ClipData(view.tag as String, mimeTypes, item)
                        var shadow = View.DragShadowBuilder(view)
                        view.startDragAndDrop(dragData, shadow, holder, 0)
                    }
                    return true
                }
            })
            holder?.imgView?.setOnClickListener {
                var intent = Intent(holder!!.ctx, ScrollGalleryActivity::class.java)
                intent.putExtra("objkey", dataset[holder!!.adapterPosition])
                holder!!.ctx.startActivity(intent)
            }

            GlobalScope.launch(Dispatchers.IO) {
                var objKey = dataset[position]
                var file = cfm.upload(objKey, null, object : CloudFileListener() {
                    override fun onError(id: Int, ex: Exception?) {
                    }

                    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                        Log.d("STATE", "Progress change called: " + bytesCurrent + "/" + bytesTotal)
                        GlobalScope.launch(Dispatchers.Main) {
                            holder!!.upArrow.visibility = View.VISIBLE
                            var pulseAnimation = android.view.animation.AnimationUtils.loadAnimation(holder!!.ctx, R.anim.pulse)
                            holder!!.upArrow.startAnimation(pulseAnimation)
                        }
                    }

                    override fun onStateChanged(id: Int, state: TransferState?) {
                    }

                    override fun onComplete(id: Int, file: File) {
                        GlobalScope.launch(Dispatchers.Main) {
                            holder!!.upArrow.clearAnimation()
                            holder!!.upArrow.visibility = View.GONE
                        }
                    }
                })
                if (file != null) {
                    GlobalScope.launch(Dispatchers.Main) {
                        holder!!.upArrow.visibility = View.VISIBLE
                        var pulseAnimation = android.view.animation.AnimationUtils.loadAnimation(holder!!.upArrow.context, R.anim.pulse)
                        holder!!.upArrow.startAnimation(pulseAnimation)
                    }
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
                    GlobalScope.launch(Dispatchers.Main) {
                        holder?.imgView?.setImageBitmap(bmap)
                    }
                } else {
                    cfm.download(objKey, object : CloudFileListener() {
                        override fun onError(id: Int, ex: Exception?) {
                        }

                        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                        }

                        override fun onStateChanged(id: Int, state: TransferState?) {
                        }

                        override fun onComplete(id: Int, file: File) {
                            async(Dispatchers.IO) {
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
                                GlobalScope.launch(Dispatchers.Main) {
                                    holder?.imgView?.setImageBitmap(bmap)
                                }
                            }
                        }
                    })
                }
            }
        }

        override fun getItemCount(): Int {
            return dataset.size
        }

        class ViewHolder: RecyclerView.ViewHolder {
            var imgView: ImageView
            var upArrow: ImageView
            var ctx: Context

            constructor(itemView: View)
                :super(itemView)
            {
                ctx = itemView.context
                imgView = itemView.findViewById(R.id.row_imgview)
                upArrow = itemView.findViewById(R.id.row_uparrow)
            }
        }
    }
}
