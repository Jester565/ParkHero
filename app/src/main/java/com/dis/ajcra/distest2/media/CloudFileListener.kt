package com.dis.ajcra.distest2.media

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import java.io.File

/**
 * Created by ajcra on 1/3/2018.
 */

abstract class CloudFileListener {
    private var priority: Int
    constructor(priority: Int = 0) {
        this.priority = priority
    }
    fun getPriority(): Int {
        return priority
    }
    open fun onError(id: Int, ex: Exception?) { }
    open fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) { }
    open fun onStateChanged(id: Int, state: TransferState?) { }
    open fun onComplete(id: Int, file: File) { }
}