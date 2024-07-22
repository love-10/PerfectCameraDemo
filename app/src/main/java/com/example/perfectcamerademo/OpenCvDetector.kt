package com.example.perfectcamerademo

import android.app.Activity
import android.content.res.Resources
import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.io.InputStream

object OpenCvDetector {
    private var mConfigBuffer: MatOfByte? = null
    private var mModelBuffer: MatOfByte? = null
    private var net: Net? = null

    fun init(activity: Activity) {
        //! [init_model_from_memory]
        mModelBuffer = loadFileFromResource(activity.resources, R.raw.mobilenet_iter_73000)
        mConfigBuffer = loadFileFromResource(activity.resources, R.raw.deploy)
        net = Dnn.readNet("caffe", mModelBuffer, mConfigBuffer)
    }

    fun run(bitmap: Bitmap): MutableList<Box> {
        val IN_WIDTH = 300
        val IN_HEIGHT = 300
        val IN_SCALE_FACTOR = 0.007843
        val MEAN_VAL = 127.5
        val THRESHOLD = 0.2

        val frame = Mat()
        Utils.bitmapToMat(bitmap, frame)
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)

        // Forward image through network.
        //! [mobilenet_handle_frame]
        val blob = Dnn.blobFromImage(
            frame, IN_SCALE_FACTOR,
            Size(IN_WIDTH.toDouble(), IN_HEIGHT.toDouble()),
            Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL),  /*swapRB*/false,  /*crop*/false
        )
        net!!.setInput(blob)
        var detections = net!!.forward()

        val cols = frame.cols()
        val rows = frame.rows()

        detections = detections.reshape(1, detections.total().toInt() / 7)
        val ret = mutableListOf<Box>()
        for (i in 0 until detections.rows()) {
            val confidence = detections[i, 2][0]
            if (confidence > THRESHOLD) {
                val classId = detections[i, 1][0].toInt()
                if (classId != 15) {
                    continue
                }
                val left = (detections[i, 3][0] * cols).toFloat()
                val top = (detections[i, 4][0] * rows).toFloat()
                val right = (detections[i, 5][0] * cols).toFloat()
                val bottom = (detections[i, 6][0] * rows).toFloat()
                ret.add(Box(left, top, right - left, bottom - top, 0f, 0))
            }
        }

        return ret
    }

    //! [mobilenet_tutorial_resource]
    private fun loadFileFromResource(resources: Resources, id: Int): MatOfByte? {
        val buffer: ByteArray
        try {
            // load cascade file from application resources
            val inputStream: InputStream = resources.openRawResource(id)

            val size = inputStream.available()
            buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return MatOfByte(*buffer)
    }
}