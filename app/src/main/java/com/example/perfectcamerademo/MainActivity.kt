package com.example.perfectcamerademo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.perfectcamerademo.databinding.ActivityMainBinding
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.size.Size
import dev.utils.app.image.BitmapUtils
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val yolo6 by lazy {
        Yolov6Ncnn().apply {
            loadModel(assets, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.cameraView.setLifecycleOwner(this)
        binding.cameraView.addCameraListener(Listener())
        binding.cameraView.setPreviewStreamSize { mutableListOf(Size(1080, 1920)) }
        binding.cameraView.frameProcessingMaxWidth = 1080
        binding.cameraView.frameProcessingMaxHeight = 1920
        binding.cameraView.post {
            val params = binding.trackView.layoutParams
            params.width = binding.cameraView.width
            params.height = binding.cameraView.height
            binding.trackView.layoutParams = params
        }
        binding.cameraView.addFrameProcessor { frame ->
            val data = frame.getData<ByteArray>()
            val yuvImage = YuvImage(
                data,
                frame.format,
                frame.size.width,
                frame.size.height,
                null
            )
            val jpegStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                Rect(
                    0, 0,
                    frame.size.width,
                    frame.size.height
                ), 100, jpegStream
            )
            val jpegByteArray = jpegStream.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(
                jpegByteArray,
                0, jpegByteArray.size
            )
            val time = System.currentTimeMillis()
            val boxes =
                yolo6.detect(BitmapUtils.reverseByHorizontal(BitmapUtils.rotate(bitmap, -90f)))
            val boundingBoxes = boxesToBoundingBoxes(boxes)
            binding.trackView.update(applyNMS(boundingBoxes))
            Log.d("xxxxx", "time: ${System.currentTimeMillis() - time}")
//            binding.sampleText.setImageBitmap()
        }

//        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.d)
//        val time = System.currentTimeMillis()
//        val boxes = yolo6.detect(bitmap)
//        val boundingBoxes: List<BoundingBox> = boxesToBoundingBoxes(boxes)
//        val bitmap1: Bitmap = drawBoundingBoxes(bitmap, boundingBoxes)
//        Log.d("xxxxx", "onCreate: ${System.currentTimeMillis() - time}")
//        binding.sampleText.setImageBitmap(bitmap1)
    }

    private inner class Listener : CameraListener() {
        override fun onCameraOpened(options: CameraOptions) {
            binding.cameraView.frameProcessingFormat = ImageFormat.JPEG
        }
    }
}