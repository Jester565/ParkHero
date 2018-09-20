package com.dis.ajcra.distest2.media

import android.arch.persistence.room.Room
import android.content.Context
import android.util.Log
import com.amazonaws.HttpMethod
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.GetObjectMetadataRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.dis.ajcra.distest2.login.CognitoManager
import kotlinx.coroutines.experimental.async
import java.io.File
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

abstract class CloudFileObserver: TransferListener {
    var utilID:AtomicInteger = AtomicInteger(-1)
    var type: TransferType
    var file: File
    private var cancelOnNoListeners: Boolean
    protected var active: Boolean = true
    private var listeners: MutableSet<CloudFileListener> = Collections.newSetFromMap(ConcurrentHashMap<CloudFileListener, Boolean>())

    constructor(type: TransferType, file: File, cancelOnNoListeners: Boolean = false) {
        this.type = type
        this.file = file
        this.cancelOnNoListeners = cancelOnNoListeners
    }

    fun checkRunning(id: Int) {
        this.utilID.set(id)
        synchronized(active) {
            if (!active) {
                cancel()
            }
        }
    }

    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
        checkRunning(id)
        for (listener in listeners) {
            listener.onProgressChanged(id, bytesCurrent, bytesTotal)
        }
    }

    override fun onStateChanged(id: Int, state: TransferState?) {
        checkRunning(id)
        for (listener in listeners) {
            listener.onStateChanged(id, state)
            if (state == TransferState.COMPLETED) {
                listener.onComplete(id, file)
            }
        }
    }

    override fun onError(id: Int, ex: java.lang.Exception?) {
        this.utilID.set(id)
        cancel()
        for (listener in listeners) {
            listener.onError(id, ex)
        }
    }

    @Synchronized open fun addListener(listener: CloudFileListener): Boolean {
        synchronized(active) {
            if (active) {
                listeners.add(listener)
                return true
            }
            return false
        }
    }

    @Synchronized open fun removeListener(listener: CloudFileListener) {
        listeners.remove(listener)
        if (cancelOnNoListeners && listeners.isEmpty()) {
            cancel()
        }
    }

    abstract fun cancel()
}

class AWSCloudFileObserver: CloudFileObserver {
    private var cfm: CloudFileManager
    private var cfi: CloudFileInfo

    constructor(cfm: CloudFileManager, cfi: CloudFileInfo, type: TransferType, file: File, cancelOnNoListeners: Boolean = false)
            :super(type, file, cancelOnNoListeners)
    {
        this.cfm = cfm
        this.cfi = cfi
        cfm.observers.put(cfi.objKey, this)
    }

    override fun onStateChanged(id: Int, state: TransferState?) {
        async {
            Log.d("CFM", "TransferState: " + state)
            if (state == TransferState.COMPLETED) {
                cfi.fileURI = file.toURI().toString()
                cfi.lastAccessed = Date().time
                cfi.lastUpdated = cfm.getLastChangedTime(cfi.objKey)
                cfm.cfiDb.cloudFileInfoDao().addCloudFileInfo(cfi)
                cancel()
            }
            super.onStateChanged(id, state)
        }
    }

    override fun cancel() {
        synchronized(active) {
            if (active) {
                active = false
                cfm.observers.remove(cfi.objKey)
            }
        }
        if (utilID.get() >= 0 && cfm.transferUtility.cancel(utilID.get())) {
            cfm.transferUtility.deleteTransferRecord(utilID.get())
        }
    }

    @Synchronized override fun addListener(listener: CloudFileListener): Boolean {
        Log.d("CFM", "Add listener called")
        if (super.addListener(listener)) {
            if (utilID.get() >= 0) {
                var transfer = cfm.transferUtility.getTransferById(utilID.get())
                if (transfer != null) {
                    listener.onStateChanged(utilID.get(), transfer.state)
                    listener.onProgressChanged(utilID.get(), transfer.bytesTransferred, transfer.bytesTotal)
                }
            }
            return true
        }
        return false
    }
}

class HttpCloudFileObserver: CloudFileObserver {
    private var cfm: CloudFileManager
    private var cfi: CloudFileInfo

    constructor(cfm: CloudFileManager, cfi: CloudFileInfo, type: TransferType, file: File, cancelOnNoListeners: Boolean = false)
            :super(type, file, cancelOnNoListeners)
    {
        this.cfm = cfm
        this.cfi = cfi
        cfm.observers.put(cfi.objKey, this)
    }

    override fun onStateChanged(id: Int, state: TransferState?) {
        async {
            if (state == TransferState.COMPLETED) {
                cfi.fileURI = file.toURI().toString()
                cfi.lastAccessed = Date().time
                cfi.lastUpdated = cfm.getLastChangedTime(cfi.objKey)
                cfm.cfiDb.cloudFileInfoDao().addCloudFileInfo(cfi)
                cancel()
            }
            super.onStateChanged(id, state)
        }
    }

    override fun cancel() {
        synchronized(active) {
            if (active) {
                active = false
                cfm.observers.remove(cfi.objKey)
            }
        }
        if (utilID.get() >= 0) {
            cfm.httpUtility.cancel(utilID.get())
        }
    }

    @Synchronized override fun addListener(listener: CloudFileListener): Boolean {
        if (super.addListener(listener)) {
            if (utilID.get() >= 0) {
                var transfer = cfm.httpUtility.getDownload(utilID.get())
                if (transfer != null) {
                    listener.onStateChanged(utilID.get(), transfer.state)
                    listener.onProgressChanged(utilID.get(), transfer.bytesCurrent, transfer.bytesTotal)
                }
            }
            return true
        }
        return false
    }
}

class CloudFileManager {
    companion object {
        var BUCKET_NAME: String = "disneyapp3"
        private var Instance: CloudFileManager? = null
        fun GetInstance(cognitoManager: CognitoManager, appContext: Context): CloudFileManager {
            if (Instance == null) {
                Instance = CloudFileManager(cognitoManager, appContext)
            }
            return Instance as CloudFileManager
        }
    }
    var observers: ConcurrentHashMap<String, CloudFileObserver> = ConcurrentHashMap<String, CloudFileObserver>()
    var transferUtility: TransferUtility
    var httpUtility: HttpDownloadUtility
    private var s3Client: AmazonS3
    private var appContext: Context
    private var cognitoManager: CognitoManager
    private var transferSubscriptions = HashMap<UUID, (String, TransferType) -> Unit>()
    var cfiDb: CloudFileDatabase

    constructor(cognitoManager: CognitoManager, appContext: Context) {
        this.cognitoManager = cognitoManager
        this.appContext = appContext
        s3Client = AmazonS3Client(cognitoManager.credentialsProvider)
        transferUtility = TransferUtility(s3Client, appContext)
        httpUtility = HttpDownloadUtility()
        cfiDb = Room.databaseBuilder(appContext, CloudFileDatabase::class.java, "cfi").build()
        async {
            initTransfers()
            displayFileInfo()
        }
    }

    private fun initTransfers() {
        run {
            var transfers = transferUtility.getTransfersWithType(TransferType.UPLOAD)
            for (transfer in transfers) {
                var file = File(transfer.absoluteFilePath)
                var cfi = getCFI(transfer.key)
                var cfo = AWSCloudFileObserver(this, cfi, TransferType.UPLOAD, file)
                cfo.onProgressChanged(transfer.id, transfer.bytesTransferred, transfer.bytesTotal)
                cfo.onStateChanged(transfer.id, transfer.state)
                transfer.setTransferListener(cfo)
                transferUtility.resume(transfer.id)
            }
        }
        run {
            var transfers = transferUtility.getTransfersWithType(TransferType.DOWNLOAD)
            for (transfer in transfers) {
                var file = File(transfer.absoluteFilePath)
                var cfi = getCFI(transfer.key)
                var cfo = AWSCloudFileObserver(this, cfi, TransferType.DOWNLOAD, file)
                cfo.onProgressChanged(transfer.id, transfer.bytesTransferred, transfer.bytesTotal)
                cfo.onStateChanged(transfer.id, transfer.state)
                transfer.setTransferListener(cfo)
            }
        }
        Log.d("CFM", "Init transfer done")
    }

    suspend fun listObjects(prefix: String, orderByDate: Boolean = false): List<S3ObjectSummary>? {
        try {
            var resp = s3Client.listObjectsV2(BUCKET_NAME, prefix)
            if (orderByDate) {
                resp.objectSummaries.sortByDescending { it ->
                    it.lastModified
                }
            }
            return resp.objectSummaries
        } catch (ex: Exception) {
            Log.d("CFM", "List object exception " + ex.message)
        }
        return null
    }

    private fun getCFI(key: String): CloudFileInfo {
        try {
            var cfi = cfiDb.cloudFileInfoDao().getCloudFileInfo(key)
            if (cfi != null) {
                Log.d("CFM", "GetCFI: " + key)
                return cfi
            }
        } catch(ex: Exception) {
            Log.d("CFM", "EX: " + ex.message)
        }
        var cfi = CloudFileInfo()
        cfi.objKey = key
        return cfi
    }

    private fun isOwnedByUser(key: String): Boolean {
        return (key.contains(cognitoManager.federatedID) || key.contains("rideAccels/") ||
                key.contains("parkIcons/") || key.contains("recs/") || key.contains("tmpProfileImgs/"))
    }

    suspend fun genPresignedURI(key: String, expireMins: Int): URI {
        var expirationDate: Date = Date()
        var msec = expirationDate.time
        msec += 1000 * 60 * expireMins //+1 hour
        expirationDate.time = msec

        var presignUrlReq = GeneratePresignedUrlRequest(BUCKET_NAME, key)
        presignUrlReq.method = HttpMethod.GET
        presignUrlReq.expiration = expirationDate

        var presignedURL = s3Client.generatePresignedUrl(presignUrlReq)
        return presignedURL.toURI()
    }

    fun getObservers(type: TransferType): Map<String, CloudFileObserver> {
        var results = HashMap<String, CloudFileObserver>()
        for (entry in observers) {
            var observer = entry.value
            if (observer.type == type || type == TransferType.ANY) {
                results.put(entry.key, observer)
            }
        }
        return results
    }

    suspend fun getCachedFile(cfi: CloudFileInfo, checkUpdate: Boolean = false): File? {
        Log.d("CFM", "Attempting to get cached file")
        if (cfi.fileURI != null) {
            Log.d("CFM", "Cache hit for " + cfi.objKey)
            var file = File(URI(cfi.fileURI))
            if (file.exists()) {
                Log.d("CFM", "file not hit")
                if (!checkUpdate || getLastChangedTime(cfi.objKey) <= cfi.lastUpdated) {
                    return file
                }
            }
        }
        return null
    }

    suspend fun getLastChangedTime(key: String): Long {
        var metaReq = GetObjectMetadataRequest(BUCKET_NAME, key)
        var metadata = s3Client.getObjectMetadata(metaReq)
        return metadata.lastModified.time
    }

    fun addTransferListener(cb: (String, TransferType) -> Unit): UUID {
        var uuid = UUID.randomUUID()
        transferSubscriptions[uuid] = cb
        return uuid
    }

    fun removeTransferListener(uuid: UUID) {
        transferSubscriptions.remove(uuid)
    }

    @Synchronized suspend fun upload(key: String, uri: URI?, listener: CloudFileListener): File? {
        //can only upload to folder owned by user
        if (isOwnedByUser(key)) {
            var cloudFileObserver = observers[key]
            if (cloudFileObserver != null) {
                //if the listener was added sucessfully
                if (cloudFileObserver.addListener(listener)) {
                    //give the file being uploaded
                    return cloudFileObserver.file
                }
            }
            //If uri is that means we are trying to listen for an upload, but since there is no upload return null
            if (uri == null) {
                return null
            }
            //We are currently not uploading, so start the real upload
            var cfi = getCFI(key)
            //TODO: Move file to non-cache directory
            var file = File(uri)
            cloudFileObserver = AWSCloudFileObserver(this, cfi, TransferType.UPLOAD, file)
            cloudFileObserver.addListener(listener)
            var awsObserver = transferUtility.upload(BUCKET_NAME, cfi.objKey, file)
            awsObserver.setTransferListener(cloudFileObserver)
            Log.d("STATE", "PRE LOOP")
            transferSubscriptions.forEach {
                Log.d("STATE", "UPLOAD LOOP")
                it.value.invoke(key, TransferType.UPLOAD)
            }
            return file
        }
        throw Error("Cannot upload to directory you do not own")
    }

    @Synchronized suspend fun download(key: String, listener: CloudFileListener, givenPresignedURI: String? = null, checkUpdate: Boolean = false) {
        //Check if we are already downloading
        run {
            var cloudFileObserver = observers[key]
            if (cloudFileObserver != null) {
                //If we listened successfully
                if (cloudFileObserver.addListener(listener)) {
                    Log.d("CFM", "Added listener to existing download")
                    return
                }
            }
        }

        //Check if the file is cached
        var cfi = getCFI(key)
        var file = getCachedFile(cfi, checkUpdate)
        if (file != null) {
            //update the last read date
            cfi.lastAccessed = Date().time
            cfiDb.cloudFileInfoDao().addCloudFileInfo(cfi)
            //call the listener with the file
            Log.d("CFM", "Got file from cache")
            listener.onComplete(0, file)
            return
        }
        Log.d("CFM", "Downloading file: " + key)
        var downloadFile = File(appContext.cacheDir, UUID.randomUUID().toString())
        downloadFile.createNewFile()
        if (isOwnedByUser(cfi.objKey)) {
            Log.d("CFM", "Owned by user")
            var awsObserver = transferUtility.download(BUCKET_NAME, cfi.objKey, downloadFile)
            var cloudFileObserver = AWSCloudFileObserver(this, cfi, TransferType.DOWNLOAD, downloadFile, true)
            cloudFileObserver.addListener(listener)
            awsObserver.setTransferListener(cloudFileObserver)
        } else {
            var presignedURI: URI
            if (givenPresignedURI == null) {
                presignedURI = genPresignedURI(cfi.objKey, 60)
            } else {
                presignedURI = URI(givenPresignedURI)
            }
            var cloudFileObserver = HttpCloudFileObserver(this, cfi, TransferType.DOWNLOAD, downloadFile, true)
            cloudFileObserver.addListener(listener)
            Log.d("CFM", "HttpDownload")
            httpUtility.download(presignedURI.toString(), downloadFile, cloudFileObserver)
        }
        transferSubscriptions.forEach {
            it.value.invoke(key, TransferType.DOWNLOAD)
        }
    }

    suspend fun clearCache(maxMB: Float = 0f) {
        //TODO: Check if there are active observers before deleting
        var megs = 0f
        var cfis = cfiDb.cloudFileInfoDao().getCloudFileInfosNewestToOldest()
        for (cfi in cfis) {
            var file = File(URI(cfi.fileURI))
            if (file.exists()) {
                var fmb = (file.length() / 1024.0f) / 1024.0f
                if (fmb + megs > maxMB) {
                    cfiDb.cloudFileInfoDao().delete(cfi)
                    file.delete()
                } else {
                    megs += fmb
                }
            } else {
                cfiDb.cloudFileInfoDao().delete(cfi)
            }
        }
    }

    @Synchronized suspend fun delete(key: String) {
        if (isOwnedByUser(key)) {
            var observer = observers[key]
            observer?.cancel()
            var cfi = getCFI(key)
            cfiDb.cloudFileInfoDao().delete(cfi)
            var file = File(URI(cfi.fileURI))
            if (file.exists()) {
                file.delete()
            }
            s3Client.deleteObject(BUCKET_NAME, cfi.objKey)
        } else {
            throw Exception("Cannot deleted key " + key + " object is not owned by user")
        }
    }

    fun displayFileInfo() {
        Log.d("CFM", "Database...")
        var cfis = cfiDb.cloudFileInfoDao().getCloudFileInfosOldestToNewest()
        for (cfi in cfis) {
            Log.d("CFM", "CFI: " + cfi)
            if (cfi.fileURI != null) {
                if (!File(URI(cfi.fileURI)).exists()) {
                    Log.d("CFM", "WARNING file does not exist")
                }
            }
            Log.d("CFM", "\n")
        }
        Log.d("CFM", "AWS Transfers...")
        var awsTransfers = transferUtility.getTransfersWithType(TransferType.ANY)
        for (transfer in awsTransfers) {
            Log.d("FI", "AWSTransfer: " + transfer.state + " : " + transfer.key + "\n")
        }
        Log.d("CFM", "Http Transfers...")
        var httpDownloads = httpUtility.getDownloads()
        for (httpDownload in httpDownloads) {
            Log.d("CFM", "HttpDownload: " + httpDownload.state + " : " + httpDownload.req.url + "\n")
        }
        Log.d("CFM", "Observers")
        for (observer in observers) {
            Log.d("CFM", "Observer: " + observer.key)
        }
    }
}
