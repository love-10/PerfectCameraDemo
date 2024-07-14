package com.example.perfectcamerademo

import android.graphics.PixelFormat
import android.os.Bundle
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.example.perfectcamerademo.databinding.ActivityMainBinding
import dev.utils.app.ScreenUtils

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var binding: ActivityMainBinding
    private val yolo6 by lazy {
        Yolo6().apply {
            loadModel(assets, 4, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraview.holder.setFormat(PixelFormat.RGBA_8888)
        binding.cameraview.holder.addCallback(this)

        val params = binding.trackView.layoutParams
        params.width = ScreenUtils.getScreenWidth()
        params.height = ScreenUtils.getScreenWidth() * 16 / 9
        binding.trackView.layoutParams = params
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        yolo6.setOutputWindow(holder.surface)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    public override fun onResume() {
        super.onResume()

        yolo6.openCamera(1, 0)
    }

    public override fun onPause() {
        super.onPause()
        yolo6.closeCamera()
    }
}