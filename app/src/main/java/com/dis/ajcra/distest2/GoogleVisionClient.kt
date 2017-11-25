package com.dis.ajcra.distest2

import android.content.Context
import android.graphics.Bitmap
import android.icu.lang.UCharacter.GraphemeClusterBreak.V
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import id.zelory.compressor.Compressor
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

/**
 * Created by ajcra on 11/21/2017.
 */

class GoogleVisionClient {
    companion object {
        var API_KEY = "AIzaSyCzPEoW_1UxlrpKAwfYy8C35JnLhckYzk0"
        var IMAGE_QUALITY = 1
    }

    class GoogleVisionRequest {
        companion object {
            val DEFAULT_FILE_NAME = "Temp"
        }
        private lateinit var ctx: Context
        private var image: Image? = null
        private var features: ArrayList<Feature> = ArrayList<Feature>()
        constructor(ctx: Context, imgData: ByteArray? = null, feature: String? = null) {
            this.ctx = ctx
            if (feature != null) {
                addFeature(feature)
            }
            if (imgData != null) {
                setImage(imgData)
            }
        }

        fun setImage(imgData: ByteArray) {
            image = Image()
            image?.encodeContent(imgData)
        }

        fun addFeature(name: String) {
            var feature = Feature()
            feature.setType(name)
            features.add(feature)
        }

        fun makeAnnotateRequest(): AnnotateImageRequest {
            var annotateRequest = AnnotateImageRequest()
            annotateRequest.setImage(image)
            annotateRequest.setFeatures(features)
            return annotateRequest
        }
    }

    lateinit var vision: Vision
    constructor() {
        var visionBuilder = Vision.Builder(
                NetHttpTransport(), AndroidJsonFactory(), null
        )
        visionBuilder.setVisionRequestInitializer(VisionRequestInitializer(API_KEY))
        vision = visionBuilder.build()
    }

    fun makeRequest(request: GoogleVisionRequest): AnnotateImageResponse {
        var batchRequest = BatchAnnotateImagesRequest()
        batchRequest.setRequests(Arrays.asList(request.makeAnnotateRequest()))
        return vision.images().annotate(batchRequest).execute().responses.get(0)
    }
}