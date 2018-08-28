package com.dis.ajcra.distest2.prof

import com.dis.ajcra.distest2.prof.Profile

/**
 * Created by ajcra on 1/25/2018.
 */
open abstract class Invite {
    var target: Profile
    var isOwner: Boolean

    constructor(target: Profile, isOwner: Boolean) {
        this.target = target
        this.isOwner = isOwner
    }

    abstract fun accept()

    abstract fun decline()

    abstract fun getType(): Int
}