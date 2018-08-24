package com.dis.ajcra.distest2.fastpass

/*
class FastPassManagerFragment : Fragment() {
    private lateinit var cognitoManager: CognitoManager
    private lateinit var cfm: CloudFileManager
    private lateinit var rideManager: RideManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RideRecyclerAdapter
    private var dataset: ArrayList<Ride> = ArrayList<Ride>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cognitoManager = CognitoManager.GetInstance(this.context.applicationContext)
        cfm = CloudFileManager.GetInstance(cognitoManager, context.applicationContext)
        adapter = RideRecyclerAdapter(cfm, dataset)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_ride_times, container, false)
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view != null) {
            recyclerView = view.findViewById(R.id.ridelist_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this@FastPassManagerFragment.context)
            recyclerView.adapter = adapter
            recyclerView.setItemViewCacheSize(50)
            recyclerView.isDrawingCacheEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        async(UI) {
            var rides = rideManager.getRides().await()
            dataset.clear()
            for (ride in rides!!) {
                dataset.add(ride)
            }
            adapter.notifyDataSetChanged()
        }
    }
}
*/