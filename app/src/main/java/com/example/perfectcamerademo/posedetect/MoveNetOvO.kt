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
    private var moveNet: PoseDetector? = null
    fun init(context: Context) {
        moveNet = MoveNet.create(context, Device.GPU, ModelType.Lightning)
        moveNet = MoveNet.create(context, Device.GPU, ModelType.Thunder)
        moveNet = PoseNet.create(context, Device.GPU)
    }

    fun run(bitmap: Bitmap): MutableList<Person> {
        return moveNet!!.estimatePoses(bitmap).toMutableList()
    }

    fun getBodyPoints(person: Person, filter: Array<BodyPart>? = bodyPoints): MutableList<PointF> {
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