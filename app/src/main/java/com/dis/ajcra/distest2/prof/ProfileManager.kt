package com.dis.ajcra.distest2.prof

import android.content.Context
import android.util.Log
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import com.dis.ajcra.distest2.DisneyAppClient
import com.dis.ajcra.distest2.entity.Entity
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.model.CreateUserInput
import com.dis.ajcra.distest2.model.SendEntityInput
import com.dis.ajcra.distest2.model.UserInfo
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.json.JSONObject

/**
 * Created by ajcra on 1/21/2018.
 */
class ProfileManager {
    var apiClient: DisneyAppClient
    var cognitoManager: CognitoManager

    companion object {
        var PROF_SETTINGS_NAME = "prof_prefs"

        fun GetInstance(appContext: Context): ProfileManager {
            if (ProfileManagerInstance == null) {
                ProfileManagerInstance = ProfileManager(CognitoManager.GetInstance(appContext), appContext)
            }
            return ProfileManagerInstance!!
        }

        private var ProfileManagerInstance: ProfileManager? = null
    }

    constructor(cognitoManager: CognitoManager, ctx: Context) {
        this.cognitoManager = cognitoManager
        val factory = ApiClientFactory()
                .credentialsProvider(cognitoManager.credentialsProvider)
        apiClient = factory.build(DisneyAppClient::class.java)
    }

    fun getPartyProfiles(): Deferred<ArrayList<Profile>> = async {
        var result = apiClient.getpartyGet()
        var partyProfiles = ArrayList<Profile>()
        result.partyMembers.forEach {
            partyProfiles.add(Profile(apiClient, it))
        }
        partyProfiles
    }

    fun genMyProfile(): Deferred<MyProfile?> = async {
        var profile = getMyProfile().await()
        Log.d("STATE", "Profile")
        if (profile == null) {
            try {
                Log.d("STATE", "Creating profile")
                profile = createMyProfile().await()
                Log.d("STATE", "Ending profile")
            } catch (ex: Exception) {
                Log.d("STATE", "Getting profile after exception")
            }
        }
        profile
    }

    fun getMyProfile(): Deferred<MyProfile?> = async {
        var myProfile: MyProfile? = null
        try {
            val output = apiClient.getuserGet(null)
            myProfile = MyProfile(apiClient, output)
        } catch (ex: Exception) {
            Log.d("STATE", "Exception: " + ex.message)
        }
        myProfile
    }

    fun getProfile(id: String, name: String? = null, profilePicUrl: String? = null): Profile {
        var profile = Profile(apiClient, id)
        profile.name = name
        profile.profilePicUrl = profilePicUrl
        return profile
    }

    fun getProfile(info: UserInfo): Profile {
        return Profile(apiClient, info)
    }

    fun getProfile(uObj: JSONObject): Profile {
        return Profile(apiClient, uObj)
    }

    fun sendEntity(objKey: String, sendToProfiles: ArrayList<Profile>): Deferred<Boolean> = async {
        var success = false
        try {
            var input = SendEntityInput()
            input.objKey = objKey
            var sendIdArr = ArrayList<String>()
            sendToProfiles.forEach {it ->
                sendIdArr.add(it.id)
            }
            input.sendToIds = sendIdArr
            apiClient.sendentityPost(input)
            success = true
        } catch (ex: Exception) {
            Log.d("STATE", "SendEntity exception: " + ex.message)
        }
        success
    }

    fun createMyProfile(): Deferred<MyProfile> = async {
        var myProfile = null
        try {
            val input = CreateUserInput()
            input.name = null
            val output = apiClient.createuserPost(input)
            var myProfile = MyProfile(apiClient, cognitoManager.federatedID)
            myProfile.name = output.name
        } catch (ex: Exception) {
            Log.d("STATE", "Create exception: " + ex.message)
        }
        myProfile as MyProfile
    }

    fun getUsers(prefix: String): Deferred<ArrayList<Profile>> = async {
        var profiles = ArrayList<Profile>()
        try {
            val output = apiClient.getusersGet("true", prefix)
            var i = 0
            while (i < output.users.size) {
                var profile = Profile(apiClient, output.users[i])
                profile.inviteStatus = output.inviteStatuses[i]
                profiles.add(profile)
                i++
            }
        } catch (ex: Exception) {
            Log.d("STATE", "Get users exception: " + ex.message)
        }

        profiles
    }

    //There may be a better place to put this
    fun getEntities(): Deferred<ArrayList<Entity>> = async {
        var entities = ArrayList<Entity>()
        try {
            var eInfos = apiClient.getentitiesGet("0")
            eInfos.forEach { it ->
                entities.add(Entity(this@ProfileManager, it))
            }
        } catch (ex: Exception) {
            Log.d("STATE", "GetEntities ex: " + ex)
        }
        entities
    }

    fun leaveParty() = async {
        try {
            apiClient.leavepartyPost()
        } catch (ex: Exception) {
            Log.d("STATE", "Could not leave party: " + ex)
        }
    }
}