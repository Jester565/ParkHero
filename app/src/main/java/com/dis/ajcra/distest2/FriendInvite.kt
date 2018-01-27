package com.dis.ajcra.distest2

import com.dis.ajcra.distest2.prof.Profile
import kotlinx.coroutines.experimental.async

/**
 * Created by ajcra on 1/25/2018.
 */
class FriendInvite: Invite {
    constructor(profile: Profile, isOwner: Boolean)
        :super(profile, isOwner)
    {
    }

    override fun accept() {
        target.addFriend()
    }

    override fun decline() {
        target.removeFriend()
    }

    override fun getType(): Int {
        return 1
    }
}