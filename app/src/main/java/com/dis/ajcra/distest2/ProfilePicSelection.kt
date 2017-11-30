package com.dis.ajcra.distest2

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.util.Log
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.experimental.async
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Button
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.experimental.withTimeout
import java.io.ByteArrayInputStream

class ProfilePicSelection : AppCompatActivity() {
    companion object {
        val REQUEST_IMAGE_CAPTURE = 1
        val FACE_DETECTION_TIMEOUT_MILLIS = 5000L
        var EXTERNAL_STORAGE_REQUEST = 2
    }

    lateinit var picView: CropImageView
    lateinit var doneButton: Button
    lateinit var googleVision: GoogleVisionClient
    lateinit var s3Cacher: S3Cacher
    lateinit var cognitoManager: CognitoManager
    var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_pic_selection)
        cognitoManager = CognitoManager.GetInstance(this)
        s3Cacher = S3Cacher(this)

        //rekClient = AmazonRekognitionClient(cognitoManager.credentialsProvider)
        googleVision = GoogleVisionClient()
        picView = findViewById(R.id.cropImageView)
        doneButton = findViewById(R.id.profpic_donebutton)
        doneButton.setOnClickListener {
            Log.d("STATE", "Done clicked")
            async {
                var bmp = picView.croppedImage
                var stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                var objKey = cognitoManager.federatedID + "/" + "profilePic.png"
                if (s3Cacher.putObj(objKey, stream.toByteArray())) {
                    Log.d("STATE", "Profile pic uploaded & cached")
                } else {
                    Log.d("STATE", "Profile pic upload failed")
                }
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            var perms = Array<String>(2, {i -> if (i==0) {Manifest.permission.READ_EXTERNAL_STORAGE} else {Manifest.permission.CAMERA}})
            ActivityCompat.requestPermissions(this,
                    perms,
                    EXTERNAL_STORAGE_REQUEST);
        } else {
            //takeProfilePic()
        }
    }

    fun exifToDegs(exifRotation: Int): Float {
        when (exifRotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> return 90.0f
            ExifInterface.ORIENTATION_ROTATE_180 -> return 180.0f
            ExifInterface.ORIENTATION_ROTATE_270 -> return 270.0f
            else -> return 0.0f
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && imageUri != null) {
            var exif = ExifInterface(contentResolver.openInputStream(imageUri))
            var rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            var degs = exifToDegs(rotation)
            var matrix = Matrix()
            if (degs != 0.0f) {
                matrix.preRotate(degs)
            }
            var imgBmp = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            imgBmp = Bitmap.createBitmap(imgBmp, 0, 0, imgBmp.width, imgBmp.height, matrix, true)
            var bmpOutputStream = ByteArrayOutputStream()
            imgBmp.compress(Bitmap.CompressFormat.WEBP, 100, bmpOutputStream)
            var bmpArr = bmpOutputStream.toByteArray()
            var bmpInputStream = ByteArrayInputStream(bmpOutputStream.toByteArray())
            var compressedBmp = BitmapFactory.decodeStream(bmpInputStream)
            picView.setImageBitmap(compressedBmp)
            async {
                var req = GoogleVisionClient.GoogleVisionRequest(this@ProfilePicSelection, bmpArr, "FACE_DETECTION")
                withTimeout(FACE_DETECTION_TIMEOUT_MILLIS) {
                    try {
                        Log.d("STATE", "Making face request")
                        var fd = googleVision.makeRequest(req)
                        Log.d("STATE", "Google vision request done")
                        if (fd.faceAnnotations.size > 0) {
                            var fa = fd.faceAnnotations.get(0)
                            if (fa.boundingPoly != null) {
                                var cropRect = Rect()
                                var firstVert = fa.boundingPoly.vertices.get(0)
                                cropRect.left = firstVert.x
                                cropRect.right = firstVert.x
                                cropRect.top = firstVert.y
                                cropRect.bottom = firstVert.y
                                var i = 1
                                while (i < fa.boundingPoly.vertices.size) {
                                    var vert = fa.boundingPoly.vertices.get(i)
                                    if (vert.x < cropRect.left) {
                                        cropRect.left = vert.x
                                    }
                                    if (vert.x > cropRect.right) {
                                        cropRect.right = vert.x
                                    }
                                    if (vert.y < cropRect.top) {
                                        cropRect.top = vert.y
                                    }
                                    if (vert.y > cropRect.bottom) {
                                        cropRect.bottom = vert.y
                                    }
                                    i++
                                }
                                runOnUiThread {
                                    setCropRect(cropRect, imgBmp.width, imgBmp.height)
                                }
                            }
                        }
                    } catch (e: Exception) {

                    }
                }
            }

            /*
            var rkImg = Image()
            rkImg.withBytes(ByteBuffer.wrap(byteArray))
            async {
                try {
                    Log.d("STATE", "Making request")
                    var detectFacesReq = DetectFacesRequest(rkImg)
                    var response = rekClient.detectFaces(detectFacesReq)
                    Log.d("STATE", "Rek response")
                    runOnUiThread {
                        picView.setImageBitmap(imgBmp)
                        Log.d("STATE", "Img Bitmap")
                        for (faceDetail in response.faceDetails) {
                            Log.d("STATE", "FACE")
                            var cropRect = Rect((imgBmp.width * faceDetail.boundingBox.left).toInt(),
                                    (imgBmp.height * faceDetail.boundingBox.top).toInt(),
                                    (imgBmp.width * (faceDetail.boundingBox.left + faceDetail.boundingBox.width)).toInt(),
                                    (imgBmp.height * (faceDetail.boundingBox.top + faceDetail.boundingBox.height)).toInt())
                            if (cropRect.width() < cropRect.height()) {
                                cropRect.left -= (cropRect.height() - cropRect.width())/2
                                cropRect.right += (cropRect.height() - cropRect.width())/2
                            } else {
                                cropRect.top -= (cropRect.width() - cropRect.height())/2
                                cropRect.bottom += (cropRect.width() - cropRect.height())/2
                            }
                            picView.cropRect = cropRect
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.d("STATE", "Exception: " + e.message)
                }
            }
            */
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            EXTERNAL_STORAGE_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //takeProfilePic()
                } else {
                    Log.d("STATE", "DENIED")
                }
                return
            }
        }
    }

    fun setCropRect(cropRect: Rect, maxWidth: Int, maxHeight: Int) {
        Log.d("STATE", "Crop Rect Set!")
        if (cropRect.width() < cropRect.height()) {
            cropRect.left -= (cropRect.height() - cropRect.width()) / 2
            cropRect.right += (cropRect.height() - cropRect.width()) / 2

            //Move rect within bounds
            if (cropRect.right > maxWidth) {
                cropRect.left -= (cropRect.right - maxWidth)
                cropRect.right = maxWidth
            } else if (cropRect.left < 0) {
                cropRect.right += -cropRect.left
                cropRect.left = 0
            }
        } else {
            cropRect.top -= (cropRect.width() - cropRect.height()) / 2
            cropRect.bottom += (cropRect.width() - cropRect.height()) / 2

            //Move rect within bounds
            if (cropRect.bottom > maxHeight) {
                cropRect.top -= (cropRect.bottom - maxHeight)
                cropRect.bottom = maxHeight
            } else if (cropRect.top < 0) {
                cropRect.bottom += -cropRect.top
                cropRect.top = 0
            }
        }
        picView.cropRect = cropRect
    }
}
