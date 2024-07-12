package com.example.perfectcamerademo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.perfectcamerademo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val yolo6 by lazy{
        Yolov6Ncnn()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        yolo6.loadModel(assets,0,0)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.d)
        val time = System.currentTimeMillis()
        val boxes=yolo6.detect(bitmap)
        val boundingBoxes: List<BoundingBox> = l2l(boxes)
        val bitmap1: Bitmap = drawBoundingBoxes(bitmap, boundingBoxes)
        Log.d("xxxxx", "onCreate: ${System.currentTimeMillis() - time}")
        binding.sampleText.setImageBitmap(bitmap1)
    }
}