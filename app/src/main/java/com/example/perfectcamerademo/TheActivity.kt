package com.example.perfectcamerademo

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import com.example.perfectcamerademo.databinding.ActivityMainBinding
import com.example.perfectcamerademo.databinding.TheActivityBinding
import com.google.gson.Gson
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import dev.utils.app.ScreenUtils

class TheActivity : AppCompatActivity() {

    private lateinit var binding: TheActivityBinding
    private val yolo6 by lazy {
        Yolo6().apply {
            loadModel(assets, 4, 0) { bitmap, boxes ->
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
        binding.cameraview.apply {
            addCameraListener(object : CameraListener() {
                override fun onCameraOpened(options: CameraOptions) {
                    super.onCameraOpened(options)
                }
            })
            addFrameProcessor {
                log("format ${it.format}")
            }
            setLifecycleOwner(this@TheActivity)
        }
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

    fun log(msg:Any?){
        Log.d("xxxxx", Gson().toJson(msg))
    }
}