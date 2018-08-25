package com.dis.ajcra.distest2.pass

import android.content.Context
import com.dis.ajcra.distest2.AppSyncTest
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.prof.ProfileManager
import com.dis.ajcra.fastpass.ListPassesQuery
import com.dis.ajcra.fastpass.fragment.DisPass
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlin.coroutines.experimental.suspendCoroutine

class PassManager {
    companion object {
        fun GetInstance(ctx: Context): PassManager {
            if (passManager == null) {
                passManager = PassManager(ctx)
            }
            return passManager!!
        }

        private var passManager: PassManager? = null
    }

    private var profileManager: ProfileManager
    private var appSync: AppSyncTest
    private var subscribers = HashSet<ListPassesCB>()

    constructor(ctx: Context) {
        appSync = AppSyncTest.getInstance(ctx)
        var cognitoManager = CognitoManager.GetInstance(ctx)
        profileManager = ProfileManager(cognitoManager)
    }

    interface ListPassesCB {
        fun passUpdated(userID: String, passes: List<DisPass>)
        fun passRemoved(passID: String)
        fun updateCompleted()
    }

    fun subscribeToPasses(cb: ListPassesCB) {
        subscribers.add(cb)
    }

    fun listPasses() {
        appSync.listPasses(object: AppSyncTest.ListPassesCallback {
            override fun onResponse(response: List<ListPassesQuery.ListPass>) {
                for (userPasses in response) {
                    var disPasses = ArrayList<DisPass>()
                    var passes = userPasses.passes()
                    if (passes != null) {
                        for (pass in passes) {
                            disPasses.add(pass.fragments().disPass())
                        }
                        for (subscriber in subscribers) {
                            subscriber.passUpdated(userPasses.user()!!, disPasses)
                        }
                    }
                }
                for (subscriber in subscribers) {
                    subscriber.updateCompleted()
                }
            }

            override fun onError(ec: Int?, msg: String?) {

            }
        })
    }

    suspend fun addPass(passID: String) = suspendCoroutine<DisPass> { cont ->
        appSync.addPass(passID, object: AppSyncTest.AddPassCallback {
            override fun onResponse(response: DisPass) {
                async(UI) {
                    var myProfile = profileManager.genMyProfile().await()
                    var dpList = arrayListOf(response)
                    for (subscriber in subscribers) {
                        subscriber.passUpdated(myProfile.id, dpList)
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

    suspend fun removePass(passID: String) = suspendCoroutine<Boolean> { cont ->
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