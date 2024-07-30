package com.example.perfectcamerademo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import com.example.perfectcamerademo.databinding.ActivityMainBinding
import com.example.perfectcamerademo.posedetect.MoveNetOvO
import dev.utils.app.ScreenUtils
import org.example.SingleKalman.log
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    fun convertToARGB8888(bitmap: Bitmap): Bitmap {
        return if (bitmap.config != Bitmap.Config.ARGB_8888) {
            val argbBitmap =
                Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(argbBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            argbBitmap
        } else {
            bitmap
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        OpenCVLoader.initLocal()
        MoveNetOvO.init(this)
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
        yolo6.openCamera(CameraFacing.BACK.ordinal, 0) { bitmap, boxes ->
            runOnUiThread {
                val time = System.currentTimeMillis()
//                val result = MoveNetOvO.run(convertToARGB8888(bitmap))
//                val points = MoveNetOvO.getBodyPoints(result.first(), null)
//                val ret = MoveNetOvO.drawPoints(bitmap, points)
                val ret = yolo6.detectFace(convertToARGB8888(bitmap))
                val ret1 = yolo6.detect(convertToARGB8888(bitmap))
                ret.addAll(ret1)
                log(ret)
                binding.trackView.update(ret)
//                binding.trackView.updatePoints(points)
                log("time ${System.currentTimeMillis() - time}")
//                log(result)
//                    binding.img.setImageBitmap(bitmap)
//                    binding.trackView.update(boxes.filter { it.label == 0 })
//                }
//                Log.d("xxxxx", "sizesize ${bitmap.width} * ${bitmap.height}")
//                Log.d("xxxxx", "box ${Gson().toJson(boxes)}")
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        yolo6.closeCamera()
    }
}