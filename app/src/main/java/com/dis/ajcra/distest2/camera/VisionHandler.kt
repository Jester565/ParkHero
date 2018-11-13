package com.dis.ajcra.distest2.camera

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.support.design.widget.BottomSheetDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.dis.ajcra.distest2.R
import com.dis.ajcra.distest2.login.CognitoManager
import com.dis.ajcra.distest2.pass.PassManager
import com.dis.ajcra.distest2.util.AnimationUtils
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async


class VisionHandler {
    private var activity: Activity
    private var inflater: LayoutInflater
    private var vision = FirebaseVision.getInstance()
    private var barcodeDetector = vision.getVisionBarcodeDetector(
            FirebaseVisionBarcodeDetectorOptions.Builder().setBarcodeFormats(FirebaseVisionBarcode.FORMAT_CODE_128).build())
    private var faceDetector: FirebaseVisionFaceDetector = vision.getVisionFaceDetector(
            FirebaseVisionFaceDetectorOptions.Builder().setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE).setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS).setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS).build())

    private var textDetector = vision.getVisionTextDetector()

    constructor(activity: Activity) {
        this.activity = activity
        this.inflater = activity.layoutInflater
    }

    fun processBmp(bmp: Bitmap) {
        var image = FirebaseVisionImage.fromBitmap(bmp)
        processHandler(image, bmp)
    }

    fun processFile(context: Context, uri: Uri) {
        var bmp = BitmapFactory.decodeFile(uri.path)
        var image = FirebaseVisionImage.fromBitmap(bmp)
        processHandler(image, bmp)
    }

    fun animateBackground(rootView: View) {
        val colorFrom = activity.resources.getColor(R.color.accent_material_dark, activity.theme)
        val colorTo = activity.resources.getColor(R.color.green, activity.theme)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 100 // milliseconds
        colorAnimation.addUpdateListener { animator -> rootView.setBackgroundColor(animator.animatedValue as Int) }
        colorAnimation.start()
    }

    fun processHandler(image: FirebaseVisionImage, bmp: Bitmap) {
        barcodeDetector.detectInImage(image).addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                //var container: ViewGroup = activity.findViewById(R.id.toastvis_layout)
                var layout: View = inflater.inflate(R.layout.toast_vision, null)
                var imgView: ImageView = layout.findViewById(R.id.toastvis_img)
                var boundBox = barcode.boundingBox
                if (boundBox != null) {
                    var width = boundBox.width() + (boundBox.width().toFloat() * 0.8f).toInt() * 2
                    boundBox.left -= (boundBox.width().toFloat() * 0.8f).toInt()
                    if (boundBox.left < 0) {
                        boundBox.left = 0
                    }
                    if (width + boundBox.left > bmp.width) {
                        width = bmp.width - boundBox.left
                    }
                    var centerY = boundBox.centerY()
                    var maxHDif = centerY
                    if (maxHDif > bmp.height - centerY) {
                        maxHDif = bmp.height - centerY
                    }
                    boundBox.top = centerY - maxHDif
                    var height = maxHDif * 2

                    imgView.setImageBitmap(Bitmap.createBitmap(bmp, boundBox.left, boundBox.top, width, height))
                }
                var extraView: TextView = layout.findViewById(R.id.toastvis_text)
                extraView.text = barcode.rawValue
                var bottomDialog = BottomSheetDialog(activity)
                bottomDialog.setContentView(layout)
                bottomDialog.show()

                var progressBar: ProgressBar = layout.findViewById(R.id.toastvis_loader)

                var button: Button = layout.findViewById(R.id.toastvis_button)
                button.setOnClickListener {
                    GlobalScope.async(Dispatchers.Main) {
                        var passManager = PassManager.GetInstance(CognitoManager.GetInstance(activity.applicationContext), activity.applicationContext)
                        var rawValue = barcode.rawValue
                        if (rawValue != null) {
                            AnimationUtils.Crossfade(progressBar, extraView, 200)
                            AnimationUtils.Crossfade(progressBar, button, 200)
                            progressBar.visibility = View.VISIBLE
                            try {
                                passManager.addPass(rawValue)
                                animateBackground(layout)
                                extraView.text = "Success!"
                                AnimationUtils.Crossfade(extraView, progressBar, 200)
                                Handler().postDelayed({
                                    bottomDialog.hide()
                                }, 5000)
                            } catch (ex: Exception) {
                                var colonI = ex.message?.lastIndexOf(":")
                                var errMsg = ex.message
                                if (colonI != null && colonI >= 0) {
                                    errMsg = errMsg?.substring(colonI + 1)
                                }
                                extraView.text = errMsg
                                AnimationUtils.Crossfade(extraView, progressBar, 200)
                                Handler().postDelayed({
                                    bottomDialog.hide()
                                }, 5000)
                            }
                        }
                    }
                }

                Log.d("VISION", "Barcode: " + barcode.rawValue)
            }
        }.addOnFailureListener { ex ->
            Log.d("VISION", "EX: " + ex.message)
        }

        faceDetector.detectInImage(image).addOnSuccessListener { faces ->
            Log.d("VISION", "face ran: " + faces.size)
            for (face in faces) {
                Log.d("VISION", "FaceDetected: " + face.smilingProbability)
            }
        }.addOnFailureListener { ex ->
            Log.d("VISION", "EX: " + ex.message)
        }

        textDetector.detectInImage(image).addOnSuccessListener { textResult ->
            for (textBlock in textResult.blocks) {
                var total = textBlock.text
                Log.d("VISION", "TextResult: " + total)
            }
        }.addOnFailureListener { ex ->
            Log.d("VISION", "EX: " + ex.message)
        }
    }
}