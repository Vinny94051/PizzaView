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
    var iconOffsetCoefficientFromCenter: Float
    var animationDuration: Long
    var increasingOffset: Float = 0f
    private var paint: Paint = Paint()
    private var centerOfX = 0f
    private var centerOfY = 0f
    private val minClickTimeInMills: Int
    private val maxClickTimeInMills: Int
    private val sectorAmount: Int
        get() = sectors.size
    private var sectors = mutableListOf<SectorModel>()
    private var clickListener: ((x: Float, y: Float) -> Unit)? = null
    private val sectorArc: Float
        get() = 360f.div(sectorAmount.toFloat())

    private var actionDownClickTime: Long = 0L
    private var actionUpClickTime: Long = 0L
    private var eventX: Float = 0f
    private var eventY: Float = 0f

    init {
        context.theme.obtainStyledAttributes(
            atrSet,
            R.styleable.circleview, 0, 0
        ).apply {
            radius = getDimension(R.styleable.circleview_circle_radius, DEFAULT_RADIUS)
            animationDuration = getFloat(
                R.styleable.circleview_animation_duration,
                DEFAULT_ANIMATION_DURATION_IN_MILLS
            )
                .toLong()
            increasingOffset = getDimension(
                R.styleable.circleview_increasing_offset,
                radius / 3
            )

            minClickTimeInMills = getInteger(
                R.styleable.circleview_min_click_time_in_mills,
                DEFAULT_MIN_CLICK_TIME_INTERVAL
            )
            maxClickTimeInMills = getInteger(
                R.styleable.circleview_max_click_time_in_mills,
                DEFAULT_MAX_CLICK_TIME_INTERVAL
            )

            val tmpIconOffset = getFloat(
                R.styleable.circleview_icon_offset_by_center_in_radius,
                DEFAULT_ICON_OFFSET_COEFFICIENT
            )
            if (tmpIconOffset in 0.1f..0.9f) {
                iconOffsetCoefficientFromCenter = tmpIconOffset
            } else {
                throw IconOffsetLengthException(
                    "Icon offset coefficient by center " +
                            " should be in 0.1f..0.9f range."
                )
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

        val (radius, increasingOffset) = when {
            widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST ->
                radius to increasingOffset
            widthSpecMode == MeasureSpec.AT_MOST -> calculateCircleParams(heightSize)
            heightSpecMode == MeasureSpec.AT_MOST -> calculateCircleParams(widthSize)
            else -> calculateCircleParams(min(heightSize, widthSize))
        }

        this.radius = radius
        this.increasingOffset = increasingOffset

        val viewSideLength: Int = ((increasingOffset + radius) * 2).toInt()
        setMeasuredDimension(viewSideLength, viewSideLength)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        initCenterPoint()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        sectors.forEach { sector ->
            sector.apply {
                drawSector(this, canvas)
            }
        }
    }

    private fun drawSector(sector: SectorModel, canvas: Canvas) {
        with(sector) {
            val increasingDelta = sector.openValueAnimator.animatedValue as Float
            canvas.drawArc(
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
            }?.draw(canvas)
        }
    }

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

    private fun calculateCircleParams(sideSize: Int): Pair<Float, Float> {
        val offset = if (increasingOffset == 0f) sideSize / 8f else increasingOffset
        val radius = sideSize / 2 - offset
        return radius to offset
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
            with(openValueAnimator) {
                if (!isActive && !isRunning) start() else reverse()
            }
            isActive = !isActive
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

        return truncate(angle / sectorArc).toInt().coerceAtMost(sectorAmount - 1)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initIcon(@DrawableRes drawable: Int, sector: SectorModel): Drawable {
        val icon: Drawable = resources.getDrawable(drawable, null)
        icon.bounds = calculateOffsetIconBounds(icon, calculateIconCenterPoint(sector))
        return icon
    }

    private inner class Point(val x: Float, val y: Float)

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun calculateOffsetIconBounds(icon: Drawable, point: Point): Rect {
        val iconCenterX = point.x
        val iconCenterY = point.y

        return Rect(
            (iconCenterX - icon.minimumWidth / 2).toInt(),
            (iconCenterY - icon.minimumHeight / 2).toInt(),
            (iconCenterX + icon.minimumWidth / 2).toInt(),
            (iconCenterY + icon.minimumHeight / 2).toInt()
        )
    }

    fun setSectorsInfo(sectorsInfo: List<SectorInfo>) {
        val sectors = mutableListOf<SectorModel>()
        sectorsInfo.forEachIndexed { index, sectorInfo ->
            sectors.add(createSector(index, sectorInfo))
        }
        this@CircleView.sectors = sectors
        invalidate()
    }

    private fun calculateIconCenterPoint(sector: SectorModel): Point {
        val x: Float
        val y: Float
        sector.apply {
            val currentRadius = radius + sector.openValueAnimator.animatedValue as Float
            //This strange angle calculation cause of different system coordinates
            val iconAngleFromCircleCenterInDegree = 90 - startAngle - sectorArc / 2
            val angleInRad = iconAngleFromCircleCenterInDegree.toRadian()
            val offsetX = (currentRadius * iconOffsetCoefficientFromCenter * sin(angleInRad))
            val offsetY = (currentRadius * iconOffsetCoefficientFromCenter * cos(angleInRad))

            x = centerOfX + offsetX
            y = centerOfY + offsetY
        }

        return Point(x, y)
    }


    class IconOffsetLengthException(message: String) : Throwable(message)

    private inner class SectorModel(
        val startAngle: Float,
        val sectorInfo: SectorInfo,
        val openValueAnimator: ValueAnimator,
        var isActive: Boolean = false,
        var icon: Drawable? = null
    )

    companion object {
        private const val DEFAULT_ANIMATION_DURATION_IN_MILLS = 200f
        private const val DEFAULT_RADIUS = 0f
        private const val DEFAULT_MIN_CLICK_TIME_INTERVAL = 0
        private const val DEFAULT_MAX_CLICK_TIME_INTERVAL = 1000
        private const val DEFAULT_ICON_OFFSET_COEFFICIENT = 0.7f
    }

}