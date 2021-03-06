package com.dis.ajcra.distest2.prof

import android.util.Log
import com.dis.ajcra.distest2.DisneyAppClient
import com.dis.ajcra.distest2.model.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONObject

/**
 * Created by ajcra on 1/18/2018.
 */
open class Profile {
    var id: String
    var name: String? = null
    var aquiredProfilePic: Boolean = false
    var profilePicUrl: String? = null
    var inviteStatus: Int = -1
    var inviteType: Int = -1
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

    constructor(apiClient: DisneyAppClient, userObj: JSONObject) {
        this.apiClient = apiClient;
        this.name = userObj.getString("name")
        this.id = userObj.getString("id")
        aquiredProfilePic = true
        this.profilePicUrl = userObj.getString("profilePicUrl")
    }

    fun getUser() = GlobalScope.async(Dispatchers.IO) {
        var output = apiClient.getuserGet(id)
        name = output.name
        aquiredProfilePic = true
        profilePicUrl = output.profilePicUrl
    }

    fun getName(): Deferred<String> = GlobalScope.async(Dispatchers.IO) {
        if (name == null) {
            getUser().await()
        }
        name as String
    }

    fun getProfilePicUrl(): Deferred<String?> = GlobalScope.async(Dispatchers.IO) {
        if (!aquiredProfilePic) {
            getUser().await()
        }
        profilePicUrl
    }

    fun getFriends(): Deferred<List<Profile>> = GlobalScope.async(Dispatchers.IO) {
        var friendArr = ArrayList<Profile>()
        try {
            var output = apiClient.getfriendsGet(id)
            for (friendInfo in output.friends) {
                friendArr.add(Profile(apiClient, friendInfo))
            }
        } catch (ex: Exception) {
            Log.d("STATE", "Could not get friends: " + ex.message)
        }
        friendArr
    }

    fun getInviteStatus(): Deferred<Int> = GlobalScope.async(Dispatchers.IO) {
        if (inviteStatus < 0) {
            try {
                var output = apiClient.getinvitestatusGet(id)
                inviteStatus = output.inviteStatus
                inviteType = output.type
            } catch (ex: Exception) {
                Log.d("STATE", "Could not get inviate status: " + ex.message)
            }
        }
        inviteStatus
    }

    fun getInviteType(): Deferred<Int> = GlobalScope.async(Dispatchers.IO) {
        if (inviteStatus < 0) {
            try {
                var output = apiClient.getinvitestatusGet(id)
                inviteStatus = output.inviteStatus
                inviteType = output.type
            } catch (ex: Exception) {
                Log.d("STATE", "Could not get inviate status: " + ex.message)
            }
        }
        inviteType
    }

    fun addFriend(): Deferred<Boolean> = GlobalScope.async(Dispatchers.IO) {
        var nowFriend = false
        try {
            var input = AddFriendInput()
            input.friendId = id
            var output = apiClient.addfriendPost(input)
            nowFriend = output.nowFriend
        } catch(ex: Exception) {
            Log.d("STATE","Add friend error: " + ex.message)
        }
        nowFriend
    }

    fun removeFriend() = GlobalScope.async(Dispatchers.IO) {
        var nowFriend = false
        try {
            var input = RemoveFriendInput()
            input.friendId = id
            apiClient.removefriendPost(input)
        } catch(ex: Exception) {
            Log.d("STATE", "Could not remove friend: " + ex.message)
        }
    }

    fun addToParty() = GlobalScope.async(Dispatchers.IO) {
        try {
            var input = AddToPartyInput()
            input.friendId = id
            apiClient.addtopartyPost(input)
        } catch(ex: Exception) {
            Log.d("STATE", "Could not add friend to party: " + ex.message)
        }
    }

    fun removePartyInvite() = GlobalScope.async(Dispatchers.IO) {
        try {
            var input = DeclinePartyInput()
            input.friendId = id
            apiClient.declinepartyPost(input)
        } catch(ex: Exception) {
            Log.d("STATE", "Could not remove party invite: " + ex.message)
        }
    }
}