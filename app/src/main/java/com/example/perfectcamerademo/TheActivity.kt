// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.
package com.example.perfectcamerademo

import android.app.Activity
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button

class TheActivity : Activity(), SurfaceHolder.Callback {
    private val yolov6ncnn = Yolo6()
    private var facing = 0
    private var cameraView: SurfaceView? = null

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraView = findViewById<View>(R.id.cameraview) as SurfaceView

        cameraView!!.holder.setFormat(PixelFormat.RGBA_8888)
        cameraView!!.holder.addCallback(this)

        val buttonSwitchCamera = findViewById<View>(R.id.buttonSwitchCamera) as Button
        buttonSwitchCamera.setOnClickListener {
            val new_facing = 1 - facing
            yolov6ncnn.closeCamera()

            yolov6ncnn.openCamera(new_facing, 0)
            facing = new_facing
        }

        reload()
    }

    private fun reload() {
        val ret_init = yolov6ncnn.loadModel(assets, 0, 0)
        if (!ret_init) {
            Log.e("MainActivity", "yolov6ncnn loadModel failed")
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        yolov6ncnn.setOutputWindow(holder.surface)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    public override fun onResume() {
        super.onResume()

        yolov6ncnn.openCamera(1, 0)
    }

    public override fun onPause() {
        super.onPause()

        yolov6ncnn.closeCamera()
    }
}
