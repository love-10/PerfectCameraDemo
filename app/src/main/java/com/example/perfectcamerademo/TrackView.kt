package com.example.perfectcamerademo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.perfectcamerademo.posedetect.MoveNet
import com.example.perfectcamerademo.posedetect.MoveNetOvO
import com.example.perfectcamerademo.tensor.VisualizationUtils

class TrackView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    val paint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    val paintCircle = Paint().apply {
        strokeWidth = 20f
        color = Color.RED
        style = Paint.Style.FILL
    }

    val paintLine = Paint().apply {
        strokeWidth = 4f
        color = Color.RED
        style = Paint.Style.STROKE
    }

    val textPaint = Paint().apply {
        color = Color.GREEN
        isAntiAlias = true
        textSize = 60f
    }
    val rects = mutableListOf<RectF>()
    val props = mutableListOf<String>()

    val points = mutableListOf<PointF>()
    val paths = mutableListOf<Path>()
    fun update(boxes: List<Box>) {
        rects.clear()
        props.clear()
        boxes.forEach {
            val scale = width / 1080
            val x1 = it.x * scale
            val y1 = it.y * scale
            val x2 = (it.x + it.width) * scale
            val y2 = (it.y + it.height) * scale
            rects.add(RectF(x1, y1, x2, y2))
            props.add("${it.prop * 100}%")
        }
        invalidate()
    }

    fun updatePoints(ps: MutableList<PointF>) {
        paths.clear()
        points.clear()
        if (ps.isEmpty()) {
            invalidate()
            return
        }
        val scale = width / 1080
        MoveNetOvO.bodyJoints.forEach {
            val pointA = ps[it.first.position]
            val pointB = ps[it.second.position]
            paths.add(Path().apply {
                this.moveTo(pointA.x * scale, pointA.y * scale)
                this.lineTo(pointB.x * scale, pointB.y * scale)
            })
        }
        this.points.addAll(ps)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = width / 1080
        rects.forEachIndexed { index, rectF ->
            canvas.drawRect(rectF, paint)
            canvas.drawText(props[index], rectF.left, rectF.top + 60, textPaint)
        }
        paths.forEach {
            canvas.drawPath(it, paintLine)
        }
        points.forEach {
            canvas.drawCircle(it.x * scale, it.y * scale, 20f, paintCircle)
        }
    }
}