package com.example.perfectcamerademo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.perfectcamerademo.databinding.TheActivityBinding
import com.google.gson.Gson
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import dev.utils.app.ScreenUtils
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

class TheActivity : AppCompatActivity() {
    // 导入 OpenCV 库

    private lateinit var binding: TheActivityBinding
    private val yolo6 by lazy {
        Yolo6().apply {
            loadModel(assets, 4, 0, false) { bitmap, boxes ->
//                runOnUiThread {
////                    binding.img.setImageBitmap(bitmap)
//                    binding.trackView.update(boxes.filter { it.label == 0 })
//                }
//                Log.d("xxxxx", "sizesize ${bitmap.width} * ${bitmap.height}")
//                Log.d("xxxxx", "box ${Gson().toJson(boxes)}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TheActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        OpenCVLoader.initLocal()
        binding.cameraview.apply {
            addCameraListener(object : CameraListener() {
                override fun onCameraOpened(options: CameraOptions) {
                    super.onCameraOpened(options)
                }
            })
            addFrameProcessor {
                //log("format ${it.format}")
            }
            setLifecycleOwner(this@TheActivity)
        }
        val Q = Mat.eye(6, 6, CvType.CV_64F).apply {
            Core.multiply(this, Scalar(0.1), this)
        }
        log(Q.get(1,1))
        val params = binding.trackView.layoutParams
        params.width = ScreenUtils.getScreenWidth()
        params.height = ScreenUtils.getScreenWidth() * 16 / 9
        binding.trackView.layoutParams = params
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
    }

    fun log(msg: Any?) {
        Log.d("xxxxx", Gson().toJson(msg))
    }
}