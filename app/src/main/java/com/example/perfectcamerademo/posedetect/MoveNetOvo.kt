package com.example.perfectcamerademo.posedetect

import android.content.Context
import android.graphics.Bitmap
import com.example.perfectcamerademo.posedetect.data.Device
import com.example.perfectcamerademo.posedetect.data.Person

object MoveNetOvo {
    private var moveNet: PoseDetector? = null
    fun init(context: Context) {
        moveNet = MoveNet.create(context, Device.GPU, ModelType.Lightning)
        moveNet = MoveNet.create(context, Device.GPU, ModelType.Thunder)
        moveNet = PoseNet.create(context, Device.GPU)
    }

    fun run(bitmap: Bitmap): MutableList<Person> {
        return moveNet!!.estimatePoses(bitmap).toMutableList()
    }
}