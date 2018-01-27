package com.dis.ajcra.distest2.prof

import android.util.Log
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import com.dis.ajcra.distest2.DisneyAppClient
import com.dis.ajcra.distest2.FriendInvite
import com.dis.ajcra.distest2.Invite
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.model.*
import com.dis.ajcra.distest2.prof.Profile
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

class MyProfile: Profile {
    constructor(apiClient: DisneyAppClient, id: String)
        :super(apiClient, id)
    {

    }

    constructor(apiClient: DisneyAppClient, userInfo: UserInfo)
            :super(apiClient, userInfo)
    {

    }

    fun rename(newName: String) = async {
        var input = RenameUserInput()
        input.name = newName
        apiClient.renameuserPost(input)
    }

    fun removeFriend(friendId: String) = async {
        var input = RemoveFriendInput()
        input.friendId = friendId
        apiClient.removefriendPost(input)
    }

    fun getInvites(): Deferred<List<Invite>> = async {
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

    fun setEndpointArn(arn: String) = async {
        try {
            var input = CreateEndpointInput()
            input.endpointArn = arn
            apiClient.createendpointPost(input)
        } catch (ex: Exception) {
            Log.d("STATE", "Set endpoint exception: " + ex.message)
        }
    }
}