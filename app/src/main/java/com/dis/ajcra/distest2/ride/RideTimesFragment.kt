package com.dis.ajcra.distest2.ride

import android.arch.persistence.room.Room
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.chip.Chip
import android.support.design.chip.ChipGroup
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import com.dis.ajcra.distest2.ParkScheduleActivity
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.accel.SensorActivity
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.media.CloudFileManager
import com.dis.ajcra.distest2.pass.PassActivity
import com.dis.ajcra.fastpass.fragment.DisRideFilter
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class RideTimesFragment: Fragment() {
    class RideFilter(var id: String, var rideIDs: List<String>) {
        var active = false
    }
    companion object {
        private val CHIP_UNCHECKED_COLORSSTATE = ColorStateList.valueOf(Color.rgb(200, 0, 0))
        private val CHIP_CHECKED_COLORSTATE = ColorStateList.valueOf(Color.rgb(255, 0, 0))
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var scheduleButton: ImageButton
    private lateinit var passButton: ImageButton
    private lateinit var timeButton: ImageButton
    private lateinit var filterButton: ImageButton

    private lateinit var filterSlider: SlidingUpPanelLayout
    private lateinit var filterTop: LinearLayout

    private var filterChips = ArrayList<Pair<Chip, RideFilter>>()
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var filterEditBar: LinearLayout
    private lateinit var filterEditButton: FloatingActionButton
    private lateinit var filterCancelButton: FloatingActionButton
    private lateinit var filterDeleteButton: FloatingActionButton

    private lateinit var navigateBar: LinearLayout
    private lateinit var filterCreateBar: LinearLayout
    private lateinit var filterCancelCreationButton: ImageButton
    private lateinit var filterEditText: EditText
    private lateinit var filterCreateButton: ImageButton

    private lateinit var cognitoManager: CognitoManager
    private lateinit var cfm: CloudFileManager
    private lateinit var rideManager: RideManager
    private lateinit var rideFilterManager: RideFilterManager
    private lateinit var adapter: RideRecyclerAdapter
    private var rides = HashMap<String, CRInfo>()
    private var adapterRides = ArrayList<CRInfo>()
    private var adapterPinnedRides = ArrayList<CRInfo>()
    private var selectedRideIDs = HashSet<String>()
    private var visibleRideIDs: HashSet<String>? = null
    private var dragging = false
    private var editingFilters = false

    private lateinit var rideDB: RideCacheDatabase

    private var clickSubID: UUID? = null

    fun addOrUpdateRideInAdapter(ride: CRInfo): Boolean {
        var arrI = addRideToAdapter(ride)
        if (arrI >= 0) {
            adapterRides.set(arrI, ride)
            adapter.notifyItemChanged(arrI)
            return false
        }
        return true
    }

    fun addRideToAdapter(ride: CRInfo): Int {
        var arrI = getAdapterRidesI(ride)
        //If less than 0, the item does not exist
        if (arrI < 0) {
            var insertI = -(arrI + 1)
            adapterRides.add(insertI, ride)
            adapter.notifyItemInserted(insertI)
            return -1
        }
        return arrI
    }

    fun getAdapterRidesI(ride: CRInfo): Int {
        if (ride != null) {
            return adapterRides.binarySearch {
                it.name.compareTo(ride.name)
            }
        }
        return -1
    }

    fun getAdapterRidesI(rideID: String): Int {
        var ride = rides.get(rideID)
        if (ride != null) {
            return getAdapterRidesI(ride)
        }
        return -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context!!.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context!!.applicationContext)
        rideManager = RideManager(cognitoManager, context!!.applicationContext)
        rideFilterManager = RideFilterManager(context!!.applicationContext)
        adapter = RideRecyclerAdapter(cfm, adapterRides, adapterPinnedRides, selectedRideIDs)

        rideDB = Room.databaseBuilder(context!!.applicationContext, RideCacheDatabase::class.java, "rides7").build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_ride_times, container, false)
        recyclerView = rootView.findViewById(R.id.ridelist_recycler)
        swipeRefreshLayout = rootView.findViewById(R.id.ridelist_swipe)
        scheduleButton = rootView.findViewById(R.id.ridelist_scheduleButton)
        passButton = rootView.findViewById(R.id.ridelist_passButton)
        timeButton = rootView.findViewById(R.id.ridelist_timeButton)

        filterButton = rootView.findViewById(R.id.ridelist_filterButton)
        filterSlider = rootView.findViewById(R.id.ridelist_filterslider)
        filterChipGroup = rootView.findViewById(R.id.ridelist_filterchips)
        filterEditBar = rootView.findViewById(R.id.ridelist_filtereditbar)
        filterEditButton = rootView.findViewById(R.id.ridelist_editfilter)
        filterCancelButton = rootView.findViewById(R.id.ridelist_cancelfilter)
        filterDeleteButton = rootView.findViewById(R.id.ridelist_deletefilter)

        navigateBar = rootView.findViewById(R.id.ridelist_navigatebar)
        filterCreateBar = rootView.findViewById(R.id.ridelist_createfilterbar)
        filterCancelCreationButton = rootView.findViewById(R.id.ridelist_cancelfiltercreation)
        filterEditText = rootView.findViewById(R.id.ridelist_filtername)
        filterCreateButton = rootView.findViewById(R.id.ridelist_createfilter)

        swipeRefreshLayout.setOnRefreshListener {
            getRides({
                swipeRefreshLayout.isRefreshing = false
            })
        }

        var cancelFilter = {
            swipeRefreshLayout.isEnabled = true
            filterChips.forEach {
                it.first.isChecked = it.second.active
            }
            filterEditBar.visibility = View.GONE
            filterChipGroup.background = ColorDrawable(Color.TRANSPARENT)
            editingFilters = false
        }

        var onFilter: (rideFilters: List<RideFilter>, added: Boolean) -> Unit = { rideFilters, added ->
            var rideIDs: Collection<String>? = null
            if (rideFilters.size > 1) {
                rideIDs = rideFilters.get(0).rideIDs.intersect(rideFilters.get(1).rideIDs)
                var i = 2
                while (i < rideFilters.size) {
                    rideIDs = rideIDs!!.intersect(rideFilters.get(i).rideIDs)
                    i++
                }

            } else if (rideFilters.size == 1) {
                rideIDs = rideFilters.get(0).rideIDs
            }
            if (rideIDs != null) {
                visibleRideIDs = HashSet(rideIDs)
                if (!added) {
                    rideIDs.forEach { rideID ->
                        var ride = rides.get(rideID)
                        if (ride != null) {
                            addRideToAdapter(ride)
                        }
                    }
                } else {
                    var i = 0
                    while (i < adapterRides.size) {
                        var ride = adapterRides.get(i)
                        if (!rideIDs.contains(ride.id)) {
                            adapterRides.removeAt(i)
                            adapter.notifyItemRemoved(i)
                            i--
                        }
                        i++
                    }
                }
            } else {
                visibleRideIDs = null
                rides.forEach { rideID, ride ->
                    addRideToAdapter(ride)
                }
            }
        }

        var onFilterEdit: (rideFilter: RideFilter) -> Unit = { rideFilter ->
            swipeRefreshLayout.isEnabled = false
            unselectAll()
            filterEditText.setText(rideFilter.id)
            //Select rides from filter
            selectedRideIDs.addAll(rideFilter.rideIDs)
            rideFilter.rideIDs.forEachIndexed { i, rideID ->
                var rideAdapterI = getAdapterRidesI(rideID)
                if (rideAdapterI >= 0) {
                    adapter.notifyItemChanged(rideAdapterI)
                }
            }
            navigateBar.visibility = View.GONE
            filterCreateBar.visibility = View.VISIBLE
            cancelFilter.invoke()
        }

        var onFilterMerge: (rideFilters: List<RideFilter>) -> Unit = { rideFilters ->
            swipeRefreshLayout.isEnabled = false
            unselectAll()
            var combinedRideIDs = HashSet<String>()
            rideFilters.forEach { rideFilter ->
                combinedRideIDs.addAll(rideFilter.rideIDs)
            }
            selectedRideIDs.addAll(combinedRideIDs)
            combinedRideIDs.forEachIndexed { i, rideID ->
                var rideAdapterI = getAdapterRidesI(rideID)
                if (rideAdapterI >= 0) {
                    adapter.notifyItemChanged(rideAdapterI)
                }
            }
            navigateBar.visibility = View.GONE
            filterCreateBar.visibility = View.VISIBLE
            cancelFilter.invoke()
        }

        var onFilterAdd: (rideFilter: DisRideFilter) -> Unit = { disRideFilter ->
            var rideFilter = RideFilter(disRideFilter.filterID()!!, disRideFilter.rideIDs()!!)
            val chip = Chip(this@RideTimesFragment.context)
            chip.chipBackgroundColor = CHIP_UNCHECKED_COLORSSTATE
            chip.text = rideFilter.id
            chip.textSize = 25f
            chip.isClickable = true
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { compoundButton, b ->
                if (chip.isChecked) {
                    chip.chipBackgroundColor = CHIP_CHECKED_COLORSTATE
                } else {
                    chip.chipBackgroundColor = CHIP_UNCHECKED_COLORSSTATE
                }
                if (!editingFilters) {
                    rideFilter.active = chip.isChecked
                    var filters = ArrayList<RideFilter>()
                    filterChips.forEach { entry ->
                        if (entry.second.active) {
                            filters.add(entry.second)
                        }
                    }
                    onFilter.invoke(filters, rideFilter.active)
                } else {
                    var checkedCount = 0
                    for (it in filterChips) {
                        if (it.first.isChecked) {
                            checkedCount++
                        }
                    }
                    if (checkedCount > 0) {
                        if (checkedCount > 1) {
                            filterEditButton.setImageResource(R.drawable.ic_merge_type_white_24dp)
                            filterEditButton.setOnClickListener {
                                var filters = ArrayList<RideFilter>()
                                filterChips.forEach {
                                    if (it.first.isChecked) {
                                        filters.add(it.second)
                                    }
                                }
                                onFilterMerge.invoke(filters)
                            }
                        } else {
                            filterEditButton.setImageResource(R.drawable.edit_icon)
                            filterEditButton.setOnClickListener {
                                var filterEntry = filterChips.find {
                                    it.first.isChecked
                                }
                                if (filterEntry != null) {
                                    onFilterEdit.invoke(filterEntry.second)
                                }
                            }
                        }
                    } else {
                        cancelFilter.invoke()
                    }
                }
            }
            chip.setOnLongClickListener {
                filterChips.forEach {
                    if (it != chip) {
                        it.first.isChecked = false
                    }
                }
                filterEditBar.visibility = View.VISIBLE
                filterChipGroup.background = ColorDrawable(Color.rgb(100, 0, 0))
                editingFilters = true
                chip.isChecked = true
                true
            }

            filterChips.add(Pair(chip, rideFilter))
            filterChipGroup.addView(chip)
        }
        rideFilterManager.subscribeToFilters(object: RideFilterManager.FilterCallback {
            override fun onFilterInit(rideFilters: List<DisRideFilter>) {
                rideFilters.forEach {
                    onFilterAdded(it)
                }
            }

            override fun onFilterAdded(rideFilter: DisRideFilter) {
                onFilterAdd.invoke(rideFilter)
            }

            override fun onFilterRemoved(filterID: String) {
                filterChips.forEach {
                    if (it.second.id == filterID) {
                        filterChipGroup.removeView(it.first)
                        filterChips.remove(it)
                        return
                    }
                }
            }

            override fun onFilterUpdated(rideFilter: DisRideFilter) {
                var filter = filterChips.find {
                    it.second.id == rideFilter.filterID()
                }
                if (filter != null) {
                    filter.second.rideIDs = rideFilter.rideIDs()!!
                    if (filter.second.active) {
                        var filters = ArrayList<RideFilter>()
                        filterChips.forEach { entry ->
                            if (entry.second.active) {
                                filters.add(entry.second)
                            }
                        }
                        onFilter.invoke(filters, true)
                    }
                } else {
                    onFilterAdd.invoke(rideFilter)
                }
            }
        })

        filterEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, startI: Int, endI: Int, count: Int) {
                if (s != null && s.length > 0) {
                    filterCreateButton.isEnabled = true
                    filterCreateButton.alpha = 1f
                    var entry = filterChips.find {
                        it.second.id == filterEditText.text.toString()
                    }
                    if (entry != null) {
                        filterCreateButton.setImageResource(R.drawable.ic_save_white_24dp)
                    } else {
                        filterCreateButton.setImageResource(R.drawable.ic_add_white_24dp)
                    }
                } else {
                    filterCreateButton.isEnabled = false
                    filterCreateButton.alpha = 0.2f
                    filterCreateButton.setImageResource(R.drawable.ic_add_white_24dp)
                }
            }

        })

        filterCreateButton.setOnClickListener {
            rideFilterManager.updateFilter(filterEditText.text.toString(), selectedRideIDs.toList())
            filterEditText.setText("")
            swipeRefreshLayout.isEnabled = true
            unselectAll()
        }

        filterCancelCreationButton.setOnClickListener {
            swipeRefreshLayout.isEnabled = true
            unselectAll()
            filterEditText.setText("")
        }

        filterCancelButton.setOnClickListener {
            cancelFilter.invoke()
        }

        filterDeleteButton.setOnClickListener {
            var filterIDs = ArrayList<String>()
            filterChips.forEach {
                if (it.first.isChecked) {
                    filterIDs.add(it.second.id)
                }
            }
            rideFilterManager.deleteFilters(filterIDs)
            cancelFilter.invoke()
        }

        filterTop = rootView.findViewById(R.id.ridelist_filterTop)
        filterTop.setOnClickListener {
            filterTop.visibility = View.GONE
            filterSlider.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        }

        rideFilterManager.listFilters()

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view != null) {
            var layoutManager = LinearLayoutManager(this@RideTimesFragment.context)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(200)

            var dragSelectTouchListener = DragSelectTouchListener().
                    withMaxScrollDistance(60).
                    withSelectListener(object: DragSelectTouchListener.OnAdvancedDragSelectListener {
                override fun onSelectionStarted(p0: Int) {

                }

                override fun onSelectionFinished(p0: Int) {
                    dragging = false
                }

                override fun onSelectChange(startI: Int, endI: Int, added: Boolean) {
                    var i = startI
                    while (i <= endI) {
                        var rideID = adapterRides.get(i)!!.id
                        if (added && !selectedRideIDs.contains(rideID)) {
                            selectedRideIDs.add(rideID)
                            adapter.notifyItemChanged(i)
                        } else if (!added && selectedRideIDs.contains(rideID)){
                            selectedRideIDs.remove(rideID)
                            adapter.notifyItemChanged(i)
                        }
                        i++
                    }
                }

            })
            recyclerView.addOnItemTouchListener(dragSelectTouchListener)
            var clickSubID = this.clickSubID
            if (clickSubID != null) {
                adapter.unsubscribeFromClicks(clickSubID)
            }
            this.clickSubID = adapter.subscribeToClicks { i, longClick ->
                var ride = adapterRides.get(i)
                if (selectedRideIDs.isEmpty() && !longClick) {
                    var rideID = ride.id
                    var intent = Intent(context, RideActivity::class.java)
                    intent.putExtra("id", rideID)
                    startActivity(intent)
                } else {
                    if (selectedRideIDs.contains(ride.id)) {
                        selectedRideIDs.remove(ride.id)
                    } else {
                        selectedRideIDs.add(ride.id)
                        if (longClick) {
                            dragSelectTouchListener.startDragSelection(i)
                        }
                    }
                    adapter.notifyItemChanged(i)
                }
                if (selectedRideIDs.isEmpty()) {
                    navigateBar.visibility = View.VISIBLE
                    filterCreateBar.visibility = View.GONE
                    swipeRefreshLayout.isEnabled = true
                } else {
                    navigateBar.visibility = View.GONE
                    filterCreateBar.visibility = View.VISIBLE
                    swipeRefreshLayout.isEnabled = false
                }
            }

            layoutManager.isAutoMeasureEnabled = true
            recyclerView.isDrawingCacheEnabled = true
            recyclerView.isNestedScrollingEnabled = false

            scheduleButton.setOnClickListener {
                var intent = Intent(context, ParkScheduleActivity::class.java)
                startActivity(intent)
            }

            passButton.setOnClickListener {
                var intent = Intent(context, PassActivity::class.java)
                startActivity(intent)
            }

            timeButton.setOnClickListener {
                var intent = Intent(context, SensorActivity::class.java)
                startActivity(intent)
            }

            filterTop.visibility = View.GONE
            filterSlider.isTouchEnabled = false
            filterButton.setOnClickListener {
                filterSlider.anchorPoint = 0.3f
                filterTop.visibility = View.VISIBLE
                filterSlider.panelState = SlidingUpPanelLayout.PanelState.ANCHORED
            }

            filterSlider.addPanelSlideListener(object: SlidingUpPanelLayout.PanelSlideListener {
                override fun onPanelSlide(panel: View?, slideOffset: Float) {

                }

                override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
                    if (newState == SlidingUpPanelLayout.PanelState.ANCHORED) {
                        filterButton.visibility = View.GONE
                    } else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED){
                        filterButton.visibility = View.VISIBLE
                    }
                }

            })
        }
    }

    private fun unselectAll() {
        var inputManager =
                context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(view!!.rootView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        var oldSelectedRideIDs = selectedRideIDs.clone() as HashSet<String>
        selectedRideIDs.clear()
        oldSelectedRideIDs.forEach { rideID ->
            var adapterRideI = getAdapterRidesI(rideID)
            if (adapterRideI >= 0) {
                adapter.notifyItemChanged(adapterRideI)
            }
        }
        navigateBar.visibility = View.VISIBLE
        filterCreateBar.visibility = View.GONE
    }

    fun handleBackButton(): Boolean {
        if (filterSlider.panelState != SlidingUpPanelLayout.PanelState.COLLAPSED) {
            filterSlider.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            filterTop.visibility = View.GONE
            return false
        }
        else if (!selectedRideIDs.isEmpty()) {
            swipeRefreshLayout.isEnabled = true
            unselectAll()
            return false
        }
        return true
    }

    fun getRides(updateCompleteCB: (() -> Unit)? = null) {
        rideManager.listRides(object: RideManager.ListRidesCB {
            override fun onAllUpdated(rides: ArrayList<CRInfo>) {
                updateCompleteCB?.invoke()
            }

            override fun init(rideUpdates: ArrayList<CRInfo>) {
                for (ride in rideUpdates) {
                    rides.put(ride.id, ride)
                    if (visibleRideIDs == null || visibleRideIDs!!.contains(ride.id)) {
                        addOrUpdateRideInAdapter(ride)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onAdd(ride: CRInfo) {
                rides.put(ride.id, ride)
                addOrUpdateRideInAdapter(ride)
            }

            override fun onUpdate(ride: CRInfo) {
                rides.put(ride.id, ride)
                addOrUpdateRideInAdapter(ride)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        getRides()
    }
}
