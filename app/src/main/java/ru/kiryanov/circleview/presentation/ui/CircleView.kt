package ru.kiryanov.circleview.presentation.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import ru.kiryanov.circleview.R
import kotlin.math.*


class CircleView(context: Context, @Nullable atrSet: AttributeSet) : View(context, atrSet) {
    var radius = 0f
    val iconOffsetByCenterInRadius: Float
    val animationDuration: Long
    var increasingOffset: Float = 0f
        get() = if (field == 0f) radius / 3 else field
    private var paint: Paint = Paint()
    private var centerOfX = 0f
    private var centerOfY = 0f
    private val minClickTimeInMills: Int
    private val maxClickTimeInMills: Int
    private var sectorAmount: Int = 0
    private var sectors = mutableListOf<SectorModel>()
    private var clickListener: ((x: Float, y: Float) -> Unit)? = null
    private val sectorArc: Float
        get() = 360f.div(sectorAmount.toFloat())


    init {
        context.theme.obtainStyledAttributes(
            atrSet,
            R.styleable.circleview, 0, 0
        ).apply {
            radius = getDimension(R.styleable.circleview_circle_radius, 0f)
            animationDuration = getFloat(R.styleable.circleview_animation_duration, 200f)
                .toLong()
            increasingOffset = getDimension(
                R.styleable.circleview_increasing_offset,
                0f
            )

            minClickTimeInMills = getInteger(
                R.styleable.circleview_min_click_time_in_mills,
                0
            )
            maxClickTimeInMills = getInteger(
                R.styleable.circleview_max_click_time_in_mills,
                1000
            )

            val tmpIconOffset = getFloat(
                R.styleable.circleview_icon_offset_by_center_in_radius,
                1.5f
            )
            if (tmpIconOffset !in 1.1f..1.9f) {
                throw IconOffsetLengthException(
                    "Icon offset by center " +
                            "in radius should be in 1.1f..1.9f range."
                )
            } else {
                iconOffsetByCenterInRadius = tmpIconOffset
            }
        }

        clickListener = { x, y ->
            findSectorByCoordinates(x, y)?.let { sectorNumber ->
                startNecessaryAnimation(sectors[sectorNumber])
            }
        }

        paint.apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, android.R.color.holo_blue_dark)
        }
        setTouchListener()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        when {
            widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST && radius == 0f -> throw RadiusNotFoundException(
                "Radius not defied"
            )
            widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST && radius != 0f -> {
                val viewSideLength = ((increasingOffset + radius) * 2).toInt()
                setMeasuredDimension(viewSideLength, viewSideLength)
            }

            widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode != MeasureSpec.AT_MOST -> {
                radius = heightSize / 3f
                setMeasuredDimension(heightSize, heightSize)
            }

            heightSpecMode == MeasureSpec.AT_MOST && widthSpecMode != MeasureSpec.AT_MOST -> {
                radius = widthSize / 3f
                setMeasuredDimension(widthSize, widthSize)
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        initCenterPoint()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        sectors.forEach { sector ->
            sector.apply {
                drawSector(this, canvas)
            }
        }
    }

    private fun drawSector(sector: SectorModel, canvas: Canvas?) {
        with(sector) {
            val increasingDelta = sector.openValueAnimator.animatedValue as Float
            canvas?.drawArc(
                RectF(
                    centerOfX - radius - increasingDelta,
                    centerOfY - radius - increasingDelta,
                    centerOfX + radius + increasingDelta,
                    centerOfY + radius + increasingDelta
                ),
                startAngle,
                sectorArc,
                true,
                paint.apply {
                    color = ContextCompat.getColor(context, sectorInfo.color)
                }
            )

            icon?.apply {
                bounds = calculateOffsetIconBounds(
                    this,
                    calculateIconCenterPoint(sector)
                )
            }?.draw(canvas!!)
        }
    }

    private var actionDownClickTime: Long = 0L
    private var actionUpClickTime: Long = 0L
    private var eventX: Float = 0f
    private var eventY: Float = 0f

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener() {
        this.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> return@setOnTouchListener handleActionDown(event)
                MotionEvent.ACTION_UP -> return@setOnTouchListener invokeClickIfNecessary()
                else -> return@setOnTouchListener false
            }
        }
    }

    private fun handleActionDown(event: MotionEvent): Boolean {
        actionDownClickTime = System.currentTimeMillis()
        eventX = event.x
        eventY = event.y
        return true
    }

    private fun invokeClickIfNecessary(): Boolean {
        actionUpClickTime = System.currentTimeMillis()
        if (isClickAction()) clickListener?.invoke(eventX, eventY)
        return true
    }

    private fun isClickAction() =
        (actionUpClickTime - actionDownClickTime) in
                minClickTimeInMills..maxClickTimeInMills

    private fun createSector(sectorNumber: Int, sectorInfo: SectorInfo): SectorModel {
        return SectorModel(
            sectorNumber * sectorArc,
            sectorInfo,
            openValueAnimator = createOpenValueAnimator()
        ).apply {
            icon = initIcon(this.sectorInfo.drawableId, this)
        }
    }

    private fun initCenterPoint() {
        centerOfX = (width / 2).toFloat()
        centerOfY = (height / 2).toFloat()
    }

    private fun createOpenValueAnimator() =
        ValueAnimator.ofFloat(0f, increasingOffset)
            .apply {
                interpolator = LinearInterpolator()
                duration = animationDuration
                addUpdateListener {
                    invalidate()
                }
            }

    private fun startNecessaryAnimation(sector: SectorModel) {
        sector.apply {
            if (!isActive) {
                if (!openValueAnimator.isRunning) {
                    isActive = true
                    openValueAnimator.start()
                }
            } else {
                isActive = false
                openValueAnimator.reverse()
            }
        }
    }

    private fun findSectorByCoordinates(x: Float, y: Float): Int? {
        if (sectors.isEmpty()) return null
        if (sectorAmount == 1) return 0
        val sectorNumber = findSector(x, y)

        val currentIncreasingOffset = sectors[sectorNumber].openValueAnimator.animatedValue as Float

        if (x in centerOfX - radius - currentIncreasingOffset..centerOfX
            + radius + currentIncreasingOffset &&
            y in centerOfY - radius - currentIncreasingOffset..centerOfY
            + radius + currentIncreasingOffset
        ) return sectorNumber

        return null
    }

    private fun findSector(x: Float, y: Float): Int {
        val angle = atan2(
            (height / 2) - y,
            (width / 2) - x
        ).toDegree() + 180
        //cause of 360 degree return sector.size => IndexBoundException
        return truncate(if (angle == 360f) 359f / sectorArc else angle / sectorArc).toInt()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initIcon(@DrawableRes drawable: Int, sector: SectorModel): Drawable {
        val icon: Drawable = resources.getDrawable(drawable, null)
        with(calculateIconCenterPoint(sector)) {
            icon.setBounds(
                (x - icon.minimumWidth).toInt() / 2,
                (y - icon.minimumHeight).toInt() / 2,
                (x + icon.minimumWidth).toInt() / 2,
                (y + icon.minimumHeight).toInt() / 2
            )
        }
        return icon
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun calculateOffsetIconBounds(icon: Drawable, point: Point): Rect {
        val iconCenterX = point.x
        val iconCenterY = point.y

        return Rect(
            (iconCenterX - icon.minimumWidth).toInt() / 2,
            (iconCenterY - icon.minimumHeight).toInt() / 2,
            (iconCenterX + icon.minimumWidth).toInt() / 2,
            (iconCenterY + icon.minimumHeight).toInt() / 2
        )
    }

    fun setSectorsInfo(sectorsInfo: List<SectorInfo>) {
        val sectors = mutableListOf<SectorModel>()
        sectorAmount = sectorsInfo.size
        for (sectorNumber in 0 until sectorAmount) {
            sectors.add(createSector(sectorNumber, sectorsInfo[sectorNumber]))
        }
        this@CircleView.sectors = sectors
        invalidate()

    }

    private fun calculateIconCenterPoint(sector: SectorModel): Point {
        val x: Float
        val y: Float
        sector.apply {
            val rad = radius + sector.openValueAnimator.animatedValue as Float
            //This strange angle calculation cause of different system coordinates
            val iconAngleFromCircleCenterInDegree = 90 - startAngle - sectorArc / 2
            val angleInRad = iconAngleFromCircleCenterInDegree.toRadian()
            val offsetX = (rad * iconOffsetByCenterInRadius * sin(angleInRad))
            val offsetY = (rad * iconOffsetByCenterInRadius * cos(angleInRad))

            x = (centerOfX * 2) + offsetX
            y = (centerOfY * 2) + offsetY
        }

        return Point(x, y)
    }

    private inner class Point(val x: Float, val y: Float)
    class RadiusNotFoundException(message: String) : Throwable(message)
    class IconOffsetLengthException(message: String) : Throwable(message)

    private inner class SectorModel(
        val startAngle: Float,
        val sectorInfo: SectorInfo,
        val openValueAnimator: ValueAnimator,
        var isActive: Boolean = false,
        var icon: Drawable? = null
    )

    private fun Float.toRadian(): Float = (this * PI / 180).toFloat()
    private fun Float.toDegree(): Float = (this / PI * 180).toFloat()
}