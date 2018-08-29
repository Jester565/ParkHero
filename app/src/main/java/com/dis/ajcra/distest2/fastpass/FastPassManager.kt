package com.dis.ajcra.distest2.fastpass

import android.content.Context
import com.dis.ajcra.distest2.AppSyncTest
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.fastpass.fragment.DisFastPassTransaction
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlin.coroutines.experimental.suspendCoroutine

class FastPassManager {
    companion object {
        fun GetInstance(cognitoManager: CognitoManager, ctx: Context): FastPassManager {
            if (fastPassManager == null) {
                fastPassManager = FastPassManager(cognitoManager, ctx)
            }
            return fastPassManager!!
        }

        private var fastPassManager: FastPassManager? = null
    }

    private var appSync: AppSyncTest
    private var subscribers = HashSet<ListPassesCB>()
    private var fastPassTransactions = HashSet<DisFastPassTransaction>()

    constructor(cognitoManager: CognitoManager, ctx: Context) {
        appSync = AppSyncTest.GetInstance(cognitoManager, ctx)
    }

    interface ListPassesCB {
        fun passUpdated(fastPass: DisFastPassTransaction)
        fun passRemoved(passID: String)
        fun updateCompleted()
    }

    fun subscribeToFastPasses(cb: ListPassesCB) {
        subscribers.add(cb)

        if (fastPassTransactions.size > 0) {
            for (fpt in fastPassTransactions) {
                cb.passUpdated(fpt)
            }
            cb.updateCompleted()
        }
    }

    fun listFastPasses() {
        appSync.listFastPasses(object: AppSyncTest.ListFastPassesCallback {
            override fun onResponse(response: List<DisFastPassTransaction>) {
                fastPassTransactions.clear()
                for (fpt in response) {
                    for (subscriber in subscribers) {
                        subscriber.passUpdated(fpt)
                    }
                    fastPassTransactions.add(fpt)
                }
                for (subscriber in subscribers) {
                    subscriber.updateCompleted()
                }
            }

            override fun onError(ec: Int?, msg: String?) { }
        })

        appSync.updateFastPasses(object: AppSyncTest.UpdateFastPassesCallback {
            override fun onResponse(response: List<DisFastPassTransaction>) {
                for (fpt in response) {
                    for (subscriber in subscribers) {
                        subscriber.passUpdated(fpt)
                    }
                    fastPassTransactions.add(fpt)
                }
                for (subscriber in subscribers) {
                    subscriber.updateCompleted()
                }
            }

            override fun onError(ec: Int?, msg: String?) { }
        })
    }

    fun addFastPass(rideID: String, passIDs: List<String>) {
        appSync.addFastPass(rideID, passIDs, object: AppSyncTest.AddFastPassCallback {
            override fun onResponse(fpt: DisFastPassTransaction) {
                for (subscriber in subscribers) {
                    subscriber.passUpdated(fpt)
                }
            }

            override fun onError(ec: Int?, msg: String?) { }
        })
    }

    suspend fun removeFastPass(passID: String) = suspendCoroutine<Boolean> { cont ->
        appSync.removePass(passID, object: AppSyncTest.RemovePassCallback {
            override fun onResponse(response: Boolean) {
                async(UI) {
                    for (subscriber in subscribers) {
                        subscriber.passRemoved(passID)
                        subscriber.updateCompleted()
                    }
                    cont.resume(response)
                }
            }

            override fun onError(ec: Int?, msg: String?) {
                cont.resumeWithException(Exception(msg))
            }
        })
    }

    fun unsubscribeFromPasses(cb: ListPassesCB) {
        subscribers.remove(cb)
    }
}