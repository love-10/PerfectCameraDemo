package com.example.perfectcamerademo.posedetect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.example.perfectcamerademo.posedetect.data.BodyPart
import com.example.perfectcamerademo.posedetect.data.Device
import com.example.perfectcamerademo.posedetect.data.Person
import com.example.perfectcamerademo.tensor.VisualizationUtils

object MoveNetOvO {
    private val bodyPoints = arrayOf(BodyPart.NOSE, BodyPart.LEFT_EYE, BodyPart.RIGHT_EYE)
    val bodyJoints = listOf(
        Pair(BodyPart.NOSE, BodyPart.LEFT_EYE),
        Pair(BodyPart.NOSE, BodyPart.RIGHT_EYE),
        Pair(BodyPart.LEFT_EYE, BodyPart.LEFT_EAR),
        Pair(BodyPart.RIGHT_EYE, BodyPart.RIGHT_EAR),
        Pair(BodyPart.NOSE, BodyPart.LEFT_SHOULDER),
        Pair(BodyPart.NOSE, BodyPart.RIGHT_SHOULDER),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW),
        Pair(BodyPart.LEFT_ELBOW, BodyPart.LEFT_WRIST),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW),
        Pair(BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_HIP),
        Pair(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP),
        Pair(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE),
        Pair(BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE),
        Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE),
        Pair(BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE)
    )
    private var moveNet: PoseDetector? = null
    fun init(context: Context) {
        moveNet = MoveNet.create(context, Device.GPU, ModelType.Lightning)
//        moveNet = MoveNet.create(context, Device.GPU, ModelType.Thunder)
//        moveNet = PoseNet.create(context, Device.GPU)
    }

    fun run(bitmap: Bitmap): MutableList<Person> {
        return moveNet!!.estimatePoses(bitmap).toMutableList()
    }

    fun getBodyPoints(person: Person, filter: Array<BodyPart>? = bodyPoints): MutableList<PointF> {
        if (person.score < 0.2) {
            return mutableListOf()
        }
        return person.keyPoints.filter {
            filter?.contains(it.bodyPart) ?: true
        }.map {
            it.coordinate
        }.toMutableList()
    }

    fun drawPoints(input: Bitmap, points: MutableList<PointF>): Bitmap {
        val paintCircle = Paint().apply {
            strokeWidth = VisualizationUtils.CIRCLE_RADIUS
            color = Color.RED
            style = Paint.Style.FILL
        }

        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val originalSizeCanvas = Canvas(output)
        points.forEach {
            originalSizeCanvas.drawCircle(
                it.x,
                it.y,
                VisualizationUtils.CIRCLE_RADIUS,
                paintCircle
            )
        }
        return output
    }
}