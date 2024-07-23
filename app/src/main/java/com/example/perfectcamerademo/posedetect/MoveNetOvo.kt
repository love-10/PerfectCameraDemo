package com.example.perfectcamerademo.posedetect

import android.content.Context
import com.example.perfectcamerademo.posedetect.data.Device

fun createMoveNet(context: Context, device: Device, modelType: ModelType): PoseDetector {
    return when (modelType) {
        ModelType.Lightning, ModelType.Thunder -> MoveNet.create(
            context,
            device,
            ModelType.Lightning
        )

        ModelType.PoseNet -> PoseNet.create(context, device)
    }
}