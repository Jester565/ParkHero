package com.dis.ajcra.distest2.prof

import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import com.dis.ajcra.distest2.DisneyAppClient
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.model.*
import com.dis.ajcra.distest2.prof.Profile
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

class MyProfile: Profile {
    class Invite {
        var isOwner: Boolean
        var targetProfile: Profile

        constructor(isOwner: Boolean, targetProfile: Profile) {
            this.isOwner = isOwner
            this.targetProfile = targetProfile
        }
    }

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

    fun addFriend(friendId: String): Deferred<Boolean> = async {
        var input = AddFriendInput()
        input.friendId = friendId
        var output = apiClient.addfriendPost(input)
        output.nowFriend
    }

    fun removeFriend(friendId: String) = async {
        var input = RemoveFriendInput()
        input.friendId = friendId
        apiClient.removefriendPost(input)
    }

    fun getInvites(): Deferred<List<Invite>> = async {
        var output = apiClient.getinvitesGet(null)
        var invArr = ArrayList<Invite>()
        for (inviteInfo in output.invites) {
            invArr.add(Invite(inviteInfo.isOwner, Profile(apiClient, inviteInfo.user)))
        }
        invArr
    }
}