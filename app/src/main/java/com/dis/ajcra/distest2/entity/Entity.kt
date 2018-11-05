package com.dis.ajcra.distest2.entity

import com.dis.ajcra.distest2.model.EntityInfo
import com.dis.ajcra.distest2.prof.Profile
import com.dis.ajcra.distest2.prof.ProfileManager
import org.json.JSONObject
import java.util.*

/**
 * Created by ajcra on 2/6/2018.
 */
class Entity {
    var id: String
    var creationTime: Date
    var owner: Profile
    var presignedUrl: String? = null
    var presignedUrlTime: Date? = null

    constructor(id: String, creationTimeStr: String, owner: Profile) {
        this.id = id
        this.creationTime = Date(creationTimeStr.toDouble().toLong())
        this.owner = owner
    }

    constructor(profileManager: ProfileManager, eInfo: EntityInfo) {
        this.id = eInfo.id
        this.presignedUrl = eInfo.url
        this.creationTime = Date(eInfo.creationTime.toDouble().toLong())
        this.owner = profileManager.getProfile(eInfo.owner)
    }

    constructor(profileManager: ProfileManager, eObj: JSONObject) {
        this.id = eObj.getString("id")
        this.presignedUrl = eObj.getString("url")
        this.creationTime = Date(eObj.getString("creationTime").toDouble().toLong())
        this.owner = profileManager.getProfile(eObj.getJSONObject("owner"))
    }
}