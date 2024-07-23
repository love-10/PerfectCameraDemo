package com.example.perfectcamerademo

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import com.example.perfectcamerademo.Kalman.KalmanPro
import com.example.perfectcamerademo.databinding.ActivityMainBinding
import com.example.perfectcamerademo.posedetect.MoveNetOvo
import com.google.gson.Gson
import dev.utils.app.ScreenUtils
import dev.utils.app.image.BitmapUtils
import org.example.SingleKalman.log
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val yolo6 by lazy {
        Rect()
        Yolo6().apply {
            loadModel(assets, 0, 0, false) { bitmap, boxes ->
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
        OpenCVLoader.initLocal()
        MoveNetOvo.init(this)
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
        OpenCvDetector.init(this)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.e)
        val time = System.currentTimeMillis()
        val result = OpenCvDetector.run(bitmap)
        log("time ${System.currentTimeMillis() - time}")
        val newList = result.map {
            FrameInfo().apply {
                addBox(it.x, it.y, it.width, it.height, it.label, it.prop)
            }
        }
        log(result)
        binding.img.setImageBitmap(DrawImage.drawResult(BitmapUtils.copy(bitmap), newList.toTypedArray(), null))
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