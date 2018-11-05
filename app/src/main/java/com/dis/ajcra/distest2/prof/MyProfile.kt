package com.dis.ajcra.distest2.prof

import android.util.Log
import com.dis.ajcra.distest2.DisneyAppClient
import com.dis.ajcra.distest2.model.CreateEndpointInput
import com.dis.ajcra.distest2.model.RemoveFriendInput
import com.dis.ajcra.distest2.model.RenameUserInput
import com.dis.ajcra.distest2.model.UserInfo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class MyProfile: Profile {
    constructor(apiClient: DisneyAppClient, id: String)
        :super(apiClient, id)
    {

    }

    constructor(apiClient: DisneyAppClient, userInfo: UserInfo)
            :super(apiClient, userInfo)
    {

    }

    fun rename(newName: String) = GlobalScope.async(Dispatchers.IO) {
        var input = RenameUserInput()
        input.name = newName
        apiClient.renameuserPost(input)
    }

    fun removeFriend(friendId: String) = GlobalScope.async(Dispatchers.IO) {
        var input = RemoveFriendInput()
        input.friendId = friendId
        apiClient.removefriendPost(input)
    }

    fun getInvites(): Deferred<List<Invite>> = GlobalScope.async(Dispatchers.IO) {
        var invArr = ArrayList<Invite>()
        try {
            var output = apiClient.getinvitesGet(null)
            for (inviteInfo in output.invites) {
                invArr.add(FriendInvite(Profile(apiClient, inviteInfo.user), inviteInfo.isOwner))
            }
        } catch (ex: Exception) {
            Log.d("STATE", " Get Invites Ex: " + ex.message)
        }
        invArr
    }

    fun setEndpointArn(arn: String) = GlobalScope.async(Dispatchers.IO) {
        try {
            var input = CreateEndpointInput()
            input.endpointArn = arn
            apiClient.createendpointPost(input)
        } catch (ex: Exception) {
            Log.d("STATE", "Set endpoint exception: " + ex.message)
        }
    }
}