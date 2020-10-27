package ru.kiryanov.circleview.presentation.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.children
import ru.kiryanov.circleview.R


class SectorView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

    private val centerAngle: Float
    private val rotateAngle: Float
    private val text: String
    private val backgroundColor: Int
    private val radius: Float
    private val oval: RectF = RectF()
    private var centerOfX: Float = 0f
    private var centerOfY: Float = 0f
    private var paint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
        style = Paint.Style.FILL
    }
    private var textView: TextView? = null


    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.sector_view,
            0, 0
        ).apply {
            centerAngle = this.getInteger(R.styleable.sector_view_center_angle, 60).toFloat()
            rotateAngle = this.getInteger(R.styleable.sector_view_rotate_angle, 40).toFloat()
            text = this.getString(R.styleable.sector_view_text).orEmpty()
            backgroundColor = this.getColor(
                R.styleable.sector_view_sector_background,
                ContextCompat.getColor(context, android.R.color.holo_green_light)
            )
            radius = this.getDimension(R.styleable.sector_view_radius, 0f)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (text.isNotEmpty()) {
            textView = TextView(context).apply {
                this.text = text
                this.textSize = 44f
                this.setTextColor(ContextCompat.getColor(context, android.R.color.holo_purple))
            }
            addView(textView)
            Log.e(javaClass.simpleName, "${textView?.parent}")
        }
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        initSectorOval()
        Log.e("$width", "::$height")
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            //  setMeasuredDimension((oval.right - oval.left).toInt()/2, (oval.bottom - oval.top).toInt()/2)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        Log.e("$width", "::$height")
    }

    override fun onDraw(canvas: Canvas) {
        Log.e(javaClass.simpleName.plus("OnDraw"), "${textView?.parent}")
        canvas.drawArc(oval, centerAngle, rotateAngle, true, paint)
        //canvas.drawCircle(centerOfX, centerOfY, 80f, paint)
        Log.e("$width", "::$height")
        super.onDraw(canvas)

    }

    private fun initSectorOval() {
        centerOfX = measuredWidth / 2f
        centerOfY = measuredHeight / 2f

        val left = centerOfX - radius
        val right = centerOfX + radius
        val top = centerOfY - radius
        val bottom = centerOfY + radius

        oval.apply {
            this.left = left
            this.right = right
            this.bottom = bottom
            this.top = top
        }
    }
}