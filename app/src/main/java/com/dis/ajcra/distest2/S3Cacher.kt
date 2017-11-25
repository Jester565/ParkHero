package com.dis.ajcra.distest2

import android.content.Context
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import android.content.Context.MODE_PRIVATE
import android.renderscript.ScriptGroup
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.common.io.Flushables.flush
import java.io.*


/**
 * Created by ajcra on 11/21/2017.
 */

class S3Cacher {
    companion object {
        val BUCKET_NAME = "disneyapp"
    }
    private lateinit var s3Client: AmazonS3Client
    private lateinit var ctx: Context
    private lateinit var lastModFile: File
    private lateinit var lastModMap: HashMap<String, Long>

    constructor(ctx: Context) {
        this.ctx = ctx
        var cognitoManager = CognitoManager.GetInstance(ctx)
        s3Client = AmazonS3Client(cognitoManager.credentialsProvider)
        lastModFile = File(ctx.filesDir, "lastModTimes")
        if (lastModFile.exists()) {
            val inputStream = ObjectInputStream(lastModFile.inputStream())
            lastModMap = inputStream.readObject() as HashMap<String, Long>
            inputStream.close()
        } else {
            lastModFile.createNewFile()
            lastModMap = HashMap<String, Long>()
            var lastModFileOut = ObjectOutputStream(lastModFile.outputStream())
            lastModFileOut.writeObject(lastModMap)
            lastModFileOut.flush()
            lastModFileOut.close()
        }
    }

    fun convertObjKeyToAndroidFSName(objKey: String): String {
        return objKey.replace('/', '%')
    }

    fun setLastModTime(objKey: String, lastModTime: Long) {
        lastModMap.put(objKey, lastModTime)
        var lastModFileOut = ObjectOutputStream(lastModFile.outputStream())
        lastModFileOut.writeObject(lastModMap)
        lastModFileOut.flush()
        lastModFileOut.close()
    }

    fun putObjNoCache(objKey: String, inputStream: InputStream, knownSize: Long = -1): Boolean {
        var size = knownSize
        if (knownSize < 0) {
            size = inputStream.readBytes().size.toLong()
        }
        var metadata = ObjectMetadata()
        metadata.contentLength = size
        var putObjReq = PutObjectRequest(BUCKET_NAME, objKey, inputStream, metadata)
        try {
            //put object in s3Bucket
            s3Client.putObject(putObjReq)
            return true
        } catch (e: AmazonServiceException) {
            return false
        } catch (e: AmazonClientException) {
            return false
        }
    }

    fun putObj(objKey: String, barr: ByteArray): Boolean {
        var fileName = convertObjKeyToAndroidFSName(objKey)
        var file = createTempFile(fileName, null, ctx.cacheDir)
        file.writeBytes(barr)
        var putObjReq = PutObjectRequest(BUCKET_NAME, objKey, file)
        //Save lastModTime
        var lastModTime = putObjReq.metadata.lastModified.time
        setLastModTime(objKey, lastModTime)
        try {
            //put object in s3Bucket
            s3Client.putObject(putObjReq)
            return true
        } catch (e: AmazonServiceException) {
            return false
        } catch (e: AmazonClientException) {
            return false
        }
    }

    fun getObjAsBytes(objKey: String, checkLastMod: Boolean = false): ByteArray? {
        var stream = getObjAsStream(objKey, checkLastMod)
        if (stream != null) {
            return stream.readBytes()
        }
        return null
    }

    fun getObjAsStream(objKey: String, checkLastMod: Boolean = false): InputStream? {
        var fileName = convertObjKeyToAndroidFSName(objKey)
        //This may be creating a file so exists doesn't work
        var file = File(ctx.cacheDir, fileName)
        if (file.exists()) {
            if (!checkLastMod) {
                return file.inputStream()
            }
            //Check if the modification time we have for the file is the most recent
            var lastModTime = lastModMap.get(objKey)
            if (lastModTime != null) {
                var metadataResp = s3Client.getObjectMetadata(BUCKET_NAME, objKey)
                if (lastModTime <= metadataResp.lastModified.time) {
                    return file.inputStream()
                }
            }
        } else {
            file.createNewFile()
        }
        try {
            var getResp = s3Client.getObject(BUCKET_NAME, objKey)
            //save in cache
            file.writeBytes(getResp.objectContent.readBytes())
            if (checkLastMod) {
                //Update the files last modification time
                var lastModTime = getResp.objectMetadata.lastModified.time
                setLastModTime(objKey, lastModTime)
            }
            //return object data
            return getResp.objectContent
        } catch (e: AmazonServiceException) {
            return null
        } catch (e: AmazonClientException) {
            return null
        }
    }

    fun getObjNoCache(objKey: String): InputStream? {
        try {
            var resp = s3Client.getObject(BUCKET_NAME, objKey)
            return resp.objectContent
        } catch (e: AmazonClientException) {
            return null
        } catch (e: AmazonServiceException) {
            return null
        }
    }
}