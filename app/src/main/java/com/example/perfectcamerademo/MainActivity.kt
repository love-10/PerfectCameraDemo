package com.example.perfectcamerademo

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import com.example.perfectcamerademo.databinding.ActivityMainBinding
import com.google.gson.Gson
import dev.utils.app.ScreenUtils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val yolo6 by lazy {
        Rect()
        Yolo6().apply {
            loadModel(assets, 0, 0, true) { bitmap, boxes ->
                runOnUiThread {
//                    binding.img.setImageBitmap(bitmap)
//                    binding.trackView.update(boxes.filter { it.label == 0 })
//                }
//                Log.d("xxxxx", "sizesize ${bitmap.width} * ${bitmap.height}")
//                Log.d("xxxxx", "box ${Gson().toJson(boxes)}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.cameraview.onSurfaceChangedListener =
            object : SelfSurfaceView.OnSurfaceChangedListener {
                override fun onSurfaceChange(surface: Surface) {
                    yolo6.setOutputWindow(surface)
                }
            }
        val params = binding.trackView.layoutParams
        params.width = ScreenUtils.getScreenWidth()
        params.height = ScreenUtils.getScreenWidth() * 16 / 9
        binding.trackView.layoutParams = params
    }

    public override fun onResume() {
        super.onResume()
        yolo6.openCamera(CameraFacing.BACK.ordinal, 0)
    }

    public override fun onPause() {
        super.onPause()
        yolo6.closeCamera()
    }
}