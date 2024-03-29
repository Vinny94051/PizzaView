package ru.kiryanov.circleview.presentation.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorRes
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
    var boundsColor: Int
    private var arcPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sectorBoundsPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var centerOfX = 0f
    private var centerOfY = 0f
    private val minClickTimeInMills: Int
    private val maxClickTimeInMills: Int
    private var sectors = mutableListOf<SectorModel>()
    private var clickListener: ((x: Float, y: Float) -> Unit)? = null
    private var sectorArc: Float = 0f
    private var eventX: Float = 0f
    private var eventY: Float = 0f
    private var onSectorClickListener: ((isActive : Boolean, sectorInfo : SectorInfo) -> Unit)? = null

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

            boundsColor = getColor(
                R.styleable.circleview_bounds_color,
                resources.getColor(android.R.color.black, null)
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

            iconOffsetCoefficientFromCenter = tmpIconOffset.coerceIn(0.4f..0.85f)
        }

        clickListener = { x, y ->
            resolveSectorClick(x, y)?.let { sectorNumber ->
                with(sectors[sectorNumber]){
                    startNecessaryAnimation(this)
                    onSectorClickListener?.invoke(isActive, sectorInfo)
                }
            }
        }

        arcPaint.apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, android.R.color.holo_blue_dark)
        }

        sectorBoundsPaint.apply {
            color = boundsColor
            strokeWidth = DEFAULT_SECTOR_BOUNDS_STROKE_WIDTH
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
            val increasingDelta = sector.sectorSizeAnimator.animatedValue as Float

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
                arcPaint.apply {
                    color = (sector.sectorColorAnimator.animatedValue as Int?)
                        ?: ContextCompat.getColor(
                            context,
                            sectorInfo.closeColor
                        )
                }
            )

            icon?.apply {
                bounds = calculateOffsetIconBounds(
                    this,
                    calculateIconCenterPoint(sector)
                )
            }?.draw(canvas)

            val (leftLineStopPoint, rightLineStopPoint) = calculateSectorBoundsTopPoints(sector)
            canvas.drawLines(
                floatArrayOf(
                    centerOfX, centerOfY, leftLineStopPoint.x, leftLineStopPoint.y,
                    centerOfX, centerOfY, rightLineStopPoint.x, rightLineStopPoint.y
                ), sectorBoundsPaint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener() {
        this.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    eventX = event.x
                    eventY = event.y
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (isClickEvent(event)) clickListener?.invoke(eventX, eventY)
                    true
                }
                else -> return@setOnTouchListener false
            }
        }
    }

    private fun isClickEvent(event: MotionEvent) =
        (event.eventTime - event.downTime) in
                minClickTimeInMills..maxClickTimeInMills

    private fun createSector(sectorNumber: Int, sectorInfo: SectorInfo): SectorModel {
        return SectorModel(
            sectorNumber * sectorArc,
            sectorInfo,
            createSectorAnimator(),
            createSectorColorChangingAnimator(sectorInfo.closeColor, sectorInfo.openColor)
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

    private fun createSectorAnimator() =
        ValueAnimator.ofFloat(0f, increasingOffset)
            .apply {
                interpolator = LinearInterpolator()
                duration = animationDuration
                addUpdateListener {
                    invalidate()
                }
            }

    private fun createSectorColorChangingAnimator(
        @ColorRes closedSectorColor: Int,
        @ColorRes openedSectorColor: Int
    ) =
        ValueAnimator.ofObject(
            ArgbEvaluator(),
            ContextCompat.getColor(context, closedSectorColor),
            ContextCompat.getColor(context, openedSectorColor)
        ).apply {
            duration = animationDuration
        }

    private fun startNecessaryAnimation(sector: SectorModel) {
        sector.apply {
            // Both of animators start and revers in one time => isRunning
            // check for any of them is valid
            if (!isActive && !sectorSizeAnimator.isRunning) {
                sectorSizeAnimator.start()
                sectorColorAnimator.start()
            } else {
                sectorSizeAnimator.reverse()
                sectorColorAnimator.reverse()
            }
            isActive = !isActive
        }
    }

    private fun resolveSectorClick(x: Float, y: Float): Int? {
        if (sectors.isEmpty()) return null
        val sectorNumber = findSectorByCoordinates(x, y)

        val currentIncreasingOffset =
            sectors[sectorNumber].sectorSizeAnimator.animatedValue as Float
        val left = centerOfX - radius - currentIncreasingOffset
        val right = centerOfX + radius + currentIncreasingOffset

        val bottom = centerOfY - radius - currentIncreasingOffset
        val top = centerOfY + radius + currentIncreasingOffset

        if (x in left..right && y in bottom..top) return sectorNumber

        return null
    }

    private fun findSectorByCoordinates(x: Float, y: Float): Int {
        val angle = atan2(
            (height / 2) - y,
            (width / 2) - x
        ).toDegree() + 180

        return truncate(angle / sectorArc).toInt()
            .coerceAtMost(sectors.size - 1)
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
        sectorArc = MAX_ANGLE_IN_DEGREE.div(sectorsInfo.size)
        sectorsInfo.forEachIndexed { index, sectorInfo ->
            sectors.add(createSector(index, sectorInfo))
        }
        this@CircleView.sectors = sectors
        invalidate()
    }

    fun setOnCircleClickListener(listener: (isActive : Boolean, sectorInfo : SectorInfo) -> Unit) {
        onSectorClickListener = listener
    }

    private fun calculateIconCenterPoint(sector: SectorModel): Point {
        val x: Float
        val y: Float
        sector.apply {
            val currentRadius = radius + sectorSizeAnimator.animatedValue as Float
            // This strange angle calculation cause of different system coordinates
            val iconAngleFromCircleCenterInDegree = 90 - startAngle - sectorArc / 2
            val angleInRad = iconAngleFromCircleCenterInDegree.toRadian()
            val offsetX = (currentRadius * iconOffsetCoefficientFromCenter * sin(angleInRad))
            val offsetY = (currentRadius * iconOffsetCoefficientFromCenter * cos(angleInRad))

            x = centerOfX + offsetX
            y = centerOfY + offsetY
        }

        return Point(x, y)
    }

    private fun calculateSectorBoundsTopPoints(sector: SectorModel): Pair<Point, Point> {
        val currentRadius = radius + sector.sectorSizeAnimator.animatedValue as Float
        val leftLineStopPoint =
            calculateLineByAngleStopPoint(sector.startAngle.toRadian(), currentRadius)
        val rightLineStopPoint = calculateLineByAngleStopPoint(
            (sector.startAngle + sectorArc).toRadian(),
            currentRadius
        )
        return leftLineStopPoint to rightLineStopPoint
    }


    private fun calculateLineByAngleStopPoint(angle: Float, currentRadius: Float): Point {
        val x = centerOfX + currentRadius * cos(angle)
        val y: Float = centerOfY + currentRadius * sin(angle)
        return Point(x, y)
    }

    private inner class SectorModel(
        val startAngle: Float,
        val sectorInfo: SectorInfo,
        val sectorSizeAnimator: ValueAnimator,
        val sectorColorAnimator: ValueAnimator,
        var isActive: Boolean = false,
        var icon: Drawable? = null
    )

    companion object {
        private const val DEFAULT_ANIMATION_DURATION_IN_MILLS = 200f
        private const val DEFAULT_RADIUS = 0f
        private const val DEFAULT_MIN_CLICK_TIME_INTERVAL = 0
        private const val DEFAULT_MAX_CLICK_TIME_INTERVAL = 1000
        private const val DEFAULT_ICON_OFFSET_COEFFICIENT = 0.7f
        private const val MAX_ANGLE_IN_DEGREE = 360f
        private const val DEFAULT_SECTOR_BOUNDS_STROKE_WIDTH = 7f
    }
}
