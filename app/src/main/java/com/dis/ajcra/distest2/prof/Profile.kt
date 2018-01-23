package com.dis.ajcra.distest2.prof

import android.util.Log
import com.dis.ajcra.distest2.DisneyAppClient
import com.dis.ajcra.distest2.model.UserInfo
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Created by ajcra on 1/18/2018.
 */
open class Profile {
    var id: String
    var name: String? = null
    var aquiredProfilePic: Boolean = false
    var profilePicUrl: String? = null
    var apiClient: DisneyAppClient

    constructor(apiClient: DisneyAppClient, id: String) {
        this.apiClient = apiClient
        this.id = id
    }

    constructor(apiClient: DisneyAppClient, userInfo: UserInfo) {
        this.apiClient = apiClient;
        this.name = userInfo.name
        this.id = userInfo.id
        aquiredProfilePic = true
        this.profilePicUrl = userInfo.profilePicUrl
    }

    fun getUser() = async {
        var output = apiClient.getuserGet(id)
        name = output.name
        aquiredProfilePic = true
        profilePicUrl = output.profilePicUrl
    }

    fun getName(): Deferred<String> = async {
        if (name == null) {
            getUser().await()
        }
        name as String
    }

    fun getProfilePicUrl(): Deferred<String?> = async {
        if (!aquiredProfilePic) {
            getUser().await()
        }
        profilePicUrl
    }

    fun getFriends(): Deferred<List<Profile>> = async {
        var output = apiClient.getfriendsGet(id)
        var friendArr = ArrayList<Profile>()
        for (friendInfo in output.friends) {
            friendArr.add(Profile(apiClient, friendInfo))
        }
        friendArr
    }

    fun getInviteStatus(): Deferred<Int> = async {
        var inviteStatus = -1
        try {
            var output = apiClient.getinvitestatusGet(id)
            inviteStatus = output.inviteStatus
        } catch (ex: Exception) {
            Log.d("STATE", "Could not get inviate status: " + ex.message)
        }
        inviteStatus
    }
}