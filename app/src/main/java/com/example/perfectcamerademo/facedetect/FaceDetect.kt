package com.example.perfectcamerademo.facedetect

import android.graphics.Bitmap
import android.graphics.ImageDecoder.ImageInfo
import com.example.perfectcamerademo.Box
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_NONE
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST
import org.example.SingleKalman.ORect
import org.example.SingleKalman.toORect


object FaceDetect {
    // 1、配置人脸检测器
    var faceDetectorOptions: FaceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(PERFORMANCE_MODE_FAST)
        .setLandmarkMode(LANDMARK_MODE_NONE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    //2、获取人脸检测器
    var detector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)

    fun run(bitmap: Bitmap, callBack: (List<Box>) -> Any) {
        // 4、处理图片
        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces ->
                val ret = faces.map {
                    val rect = it.boundingBox
                    Box(
                        rect.left.toFloat(),
                        rect.top.toFloat(),
                        rect.right - rect.left.toFloat(),
                        rect.bottom.toFloat() - rect.top.toFloat(),
                        0f,
                        0
                    )
                }
                callBack.invoke(ret)
            }
            .addOnFailureListener { }
    }

}