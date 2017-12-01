package com.dis.ajcra.distest2

import android.content.Context
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import android.content.Context.MODE_PRIVATE
import android.renderscript.ScriptGroup
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.common.io.Flushables.flush
import java.io.*
import java.lang.Exception


/**
 * Created by ajcra on 11/21/2017.
 */

class S3Cacher {
    companion object {
        val BUCKET_NAME = "disneyapp"
    }
    /*
    private lateinit var ctx: Context
    private lateinit var transferUtil: TransferUtility

    constructor(appCtx: Context) {
        this.ctx = appCtx
        var cognitoManager = CognitoManager.GetInstance(ctx)
        var s3Client = AmazonS3Client(cognitoManager.credentialsProvider)
        transferUtil = TransferUtility(s3Client, this.ctx)
    }

    fun convertObjKeyToAndroidFSName(objKey: String): String {
        return objKey.replace('/', '%')
    }

    fun putObj(objKey: String, barr: ByteArray): Boolean {
        var fileName = convertObjKeyToAndroidFSName(objKey)
        var file = File(ctx.filesDir, fileName)
        file.writeBytes(barr)

        var observer = transferUtil.upload(BUCKET_NAME, objKey, file)
        observer.setTransferListener(object: TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

            }

            override fun onStateChanged(id: Int, state: TransferState?) {

            }

            override fun onError(id: Int, ex: Exception?) {

            }
        })
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
    */
}