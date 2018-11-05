package com.dis.ajcra.distest2.ride

import android.content.Context
import android.util.Log
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ResponseFetcher
import com.dis.ajcra.distest2.AppSyncTest
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.fastpass.AddRideFilterMutation
import com.dis.ajcra.fastpass.DeleteRideFilterMutation
import com.dis.ajcra.fastpass.ListRideFiltersQuery
import com.dis.ajcra.fastpass.fragment.DisNotify
import com.dis.ajcra.fastpass.fragment.DisRideFilter
import com.dis.ajcra.fastpass.type.NotifyInput
import kotlinx.coroutines.*
import java.util.*


class RideFilterManager {
    interface FilterCallback {
        fun onFilterInit(rideFilter: List<DisRideFilter>)
        fun onFilterAdded(rideFilter: DisRideFilter)
        fun onFilterRemoved(filterID: String)
        fun onFilterUpdated(rideFilter: DisRideFilter)
    }
    private var appSyncManager: AppSyncTest
    private var client: AWSAppSyncClient
    private var rideFilters = HashMap<String, DisRideFilter>()
    private var filterSubscriptions = HashMap<UUID, FilterCallback>()

    constructor(appContext: Context) {
        var cognitoManager = CognitoManager.GetInstance(appContext)
        appSyncManager = AppSyncTest.GetInstance(cognitoManager, appContext)
        client = appSyncManager.getClient()
    }

    fun subscribeToFilters(filterCB: FilterCallback): Deferred<UUID> = GlobalScope.async(Dispatchers.Main) {
        var subID = UUID.randomUUID()
        filterCB.onFilterInit(rideFilters.values.toList())
        filterSubscriptions.put(subID, filterCB)
        subID
    }

    fun updateFilter(filterID: String, rideIDs: List<String>, notifyConfig: NotifyInput? = null) {
        var disNotifyConfig: DisRideFilter.NotifyConfig? = null
        if (notifyConfig != null) {
            disNotifyConfig = DisRideFilter.NotifyConfig("NotifyConfig",
                    DisRideFilter.NotifyConfig.Fragments(DisNotify(
                            "NotifyConfig", notifyConfig.waitRating(), notifyConfig.waitTime(), notifyConfig.fastPassTime(),
                            notifyConfig.distance(), notifyConfig.inLineTime())
                    ))
        }
        var disRideFilter = DisRideFilter("RideFilter", filterID, rideIDs, disNotifyConfig)

        //Check if updated or added and call appropiate callback
        var existingRideFilter = rideFilters.get(filterID)
        rideFilters.put(filterID, disRideFilter)
        if (existingRideFilter != null) {
            filterSubscriptions.forEach { uuid, filterCallback ->
                filterCallback.onFilterUpdated(disRideFilter)
            }
        } else {
            filterSubscriptions.forEach { uuid, filterCallback ->
                filterCallback.onFilterAdded((disRideFilter))
            }
        }

        //Optimistically update the cache
        var listRideFiltersQuery = ListRideFiltersQuery.builder().build()
        client.query(listRideFiltersQuery)
                .responseFetcher(AppSyncResponseFetchers.CACHE_ONLY)
                .enqueue(object: GraphQLCall.Callback<ListRideFiltersQuery.Data>() {
                    override fun onFailure(e: ApolloException) {
                        Log.e("RIDEFILTER", "LISTRIDEFILTERS OPTIMISTIC UPDATE ERROR: " + e.message)
                    }

                    override fun onResponse(response: Response<ListRideFiltersQuery.Data>) {
                        var newList = ArrayList<ListRideFiltersQuery.GetRideFilter>()
                        response.data()?.rideFilters?.forEach {
                            //Don't add old RideFilter, we are about to replace it
                            if (it.fragments().disRideFilter().filterID() != filterID) {
                                newList.add(it)
                            }
                        }
                        newList.add(ListRideFiltersQuery.GetRideFilter("RideFilter",
                                ListRideFiltersQuery.GetRideFilter.Fragments(disRideFilter)))
                        var newRideFilterListData = ListRideFiltersQuery.Data(newList)
                        try {
                            client.getStore().write(listRideFiltersQuery, newRideFilterListData).execute()
                        } catch (e: ApolloException) {
                            Log.e("RIDEFILTER", "Failed to update ListRideFilters query optimistically", e)
                        }

                    }
                })

        client.mutate(AddRideFilterMutation.builder().filterID(filterID).rideIDs(rideIDs).notifyConfig(notifyConfig).build())
                .enqueue(object: GraphQLCall.Callback<AddRideFilterMutation.Data>() {
                    override fun onFailure(e: ApolloException) {
                        Log.e("RIDEFILTER", "ADDRIDEFILTER ERROR: " + e.message)
                    }

                    override fun onResponse(response: Response<AddRideFilterMutation.Data>) {
                        if (response.hasErrors()) {
                            Log.e("RIDEFILTER", "ADDRIDEFILTERS PARSE ERROR!")
                            var errRes = appSyncManager.parseRespErrs(response as Response<Any>)
                        }
                    }
                })
    }

    fun deleteFilter(filterID: String) = GlobalScope.async(Dispatchers.Main) {
        if (rideFilters.remove(filterID) != null) {
            filterSubscriptions.forEach { uuid, filterCallback ->
                filterCallback.onFilterRemoved(filterID)
            }
            //Optimistically update the cache
            var listRideFiltersQuery = ListRideFiltersQuery.builder().build()
            client.query(listRideFiltersQuery)
                    .responseFetcher(AppSyncResponseFetchers.CACHE_ONLY)
                    .enqueue(object : GraphQLCall.Callback<ListRideFiltersQuery.Data>() {
                        override fun onFailure(e: ApolloException) {
                            Log.e("RIDEFILTER", "LISTRIDEFILTERS OPTIMISTIC UPDATE ERROR: " + e.message)
                        }

                        override fun onResponse(response: Response<ListRideFiltersQuery.Data>) {
                            var newList = ArrayList<ListRideFiltersQuery.GetRideFilter>()
                            response.data()?.rideFilters?.forEach {
                                //Don't add old RideFilter, we are about to replace it
                                if (it.fragments().disRideFilter().filterID() != filterID) {
                                    newList.add(it)
                                }
                            }
                            var newRideFilterListData = ListRideFiltersQuery.Data(newList)
                            try {
                                client.getStore().write(listRideFiltersQuery, newRideFilterListData).execute()
                            } catch (e: ApolloException) {
                                Log.e("RIDEFILTER", "Failed to update ListRideFilters query optimistically", e)
                            }

                        }
                    })
            Log.d("RIDEFILTER", "MUTATE: " + filterID)
            client.mutate(DeleteRideFilterMutation.builder().filterID(filterID).build())
                    .enqueue(object : GraphQLCall.Callback<DeleteRideFilterMutation.Data>() {
                        override fun onFailure(e: ApolloException) {
                            Log.e("RIDEFILTER", "DELETERIDEFILTER ERROR: " + e.message)
                        }

                        override fun onResponse(response: Response<DeleteRideFilterMutation.Data>) {
                            if (response.hasErrors()) {
                                Log.e("RIDEFILTER", "DELETERIDEFILTERS PARSE ERROR!")
                                var errRes = appSyncManager.parseRespErrs(response as Response<Any>)
                            }
                        }
                    })
        }
    }

    fun listFilters(fetcher: ResponseFetcher = AppSyncResponseFetchers.CACHE_FIRST) {
        client.query(ListRideFiltersQuery.builder().build())
                .responseFetcher(fetcher)
                .enqueue(object: GraphQLCall.Callback<ListRideFiltersQuery.Data>() {
                    override fun onFailure(e: ApolloException) {
                        Log.e("RIDEFILTER", "LISTRIDEFILTERS ERROR: " + e.message)
                    }

                    override fun onResponse(response: Response<ListRideFiltersQuery.Data>) {
                        GlobalScope.launch(Dispatchers.Main) {
                            if (!response.hasErrors()) {
                                var dataRideFilters = response.data()?.rideFilters
                                dataRideFilters?.forEach { it ->
                                    Log.d("RIDEFILTER", "TYPE1: " + it.__typename() +
                                            "  TYPE2: " + it.fragments().disRideFilter().__typename() +
                                            "  Type3: " + it.fragments().disRideFilter().notifyConfig()?.__typename())
                                    var disRideFilter = it.fragments().disRideFilter()
                                    var existingRideFilter = rideFilters.get(disRideFilter.filterID())
                                    rideFilters.put(disRideFilter.filterID()!!, disRideFilter)
                                    if (existingRideFilter != null) {
                                        filterSubscriptions.forEach { uuid, filterCallback ->
                                            filterCallback.onFilterUpdated(disRideFilter)
                                        }
                                    } else {
                                        filterSubscriptions.forEach { uuid, filterCallback ->
                                            filterCallback.onFilterAdded(disRideFilter)
                                        }
                                    }
                                }
                            } else {
                                Log.e("RIDEFILTER", "LISTRIDEFILTERS PARSE ERROR!")
                                var errRes = appSyncManager.parseRespErrs(response as Response<Any>)
                            }
                        }
                    }
                })
    }
}