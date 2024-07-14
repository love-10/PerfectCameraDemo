package com.example.perfectcamerademo

import android.os.Bundle
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import com.example.perfectcamerademo.databinding.ActivityMainBinding
import dev.utils.app.ScreenUtils

class MainActivity : AppCompatActivity() {

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