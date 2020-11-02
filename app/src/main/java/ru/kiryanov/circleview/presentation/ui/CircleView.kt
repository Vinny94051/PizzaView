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
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import ru.kiryanov.circleview.R
import kotlin.math.*


class CircleView(context: Context, @Nullable atrSet: AttributeSet) : View(context, atrSet) {

    var paint: Paint = Paint()
    var centerOfX = 0f
    var centerOfY = 0f
    var radius = 0f
    val iconOffsetByCenterInRadius: Float
    val animationDuration: Long
    var increasingOffset: Float = 0f

    private val minClickTimeInMills: Int
    private val maxClickTimeInMills: Int
    private var sectorAmount: Int
    private var sectors = mutableListOf<SectorModel>()

    private var clickListener: ((x: Float, y: Float) -> Unit)? = null
    private val sectorArc: Float
        get() {
            return 360f.div(sectorAmount.toFloat())
        }


    init {
        context.theme.obtainStyledAttributes(
            atrSet,
            R.styleable.circleview, 0, 0
        ).apply {
            sectorAmount = getInteger(R.styleable.circleview_sector_amount, 1)
            radius = getDimension(R.styleable.circleview_circle_radius, 0f)
            animationDuration = getFloat(R.styleable.circleview_animation_duration, 200f)
                .toLong()
            increasingOffset = getDimension(
                R.styleable.circleview_increasing_offset,
                radius / 3
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
                throw IconOffsetByCenterInRadiusException(
                    "Icon offset by center " +
                            "in radius should be in 1.1f..1.9f range."
                )
            } else {
                iconOffsetByCenterInRadius = tmpIconOffset
            }
        }

        clickListener = { x, y ->
            val secNum = findSectorByCoordinates(x, y)
            secNum?.let { num ->
                startNecessaryAnimation(sectors[num])
            }
        }

        paint.apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, android.R.color.holo_blue_dark)
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val viewSideLength = ((increasingOffset + radius) * 2).toInt()
        setMeasuredDimension(viewSideLength, viewSideLength)
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
            canvas?.drawArc(
                RectF(
                    centerOfX - radius - sector.currentIncreasingDelta,
                    centerOfY - radius - sector.currentIncreasingDelta,
                    centerOfX + radius + sector.currentIncreasingDelta,
                    centerOfY + radius + sector.currentIncreasingDelta
                ),
                startAngle,
                sweepAngle,
                true,
                paint.apply {
                    color = ContextCompat.getColor(context, sectorInfo.color)
                }
            )

            icon?.apply {
                mutableRadius = radius + sector.currentIncreasingDelta
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
            if (event.action == MotionEvent.ACTION_DOWN) {
                actionDownClickTime = System.currentTimeMillis()
                eventX = event.x
                eventY = event.y
                return@setOnTouchListener true
            } else if (event.action == MotionEvent.ACTION_UP) {
                actionUpClickTime = System.currentTimeMillis()
                if (isClickAction()) {
                    clickListener?.invoke(eventX, eventY)
                }
                return@setOnTouchListener true
            }

            false
        }
    }

    private fun isClickAction() =
        (actionUpClickTime - actionDownClickTime) in
                minClickTimeInMills..maxClickTimeInMills

    private fun createSector(sectorNumber: Int, sectorInfo: SectorInfo): SectorModel {
        return SectorModel(
            sectorNumber * sectorArc,
            sectorArc,
            sectorInfo,
            mutableRadius = radius
        ).apply {
            icon = initIcon(this.sectorInfo.drawableId, this)
            openValueAnimator = createOpenValueAnimator(this)
        }
    }

    private fun initCenterPoint() {
        centerOfX = (width / 2).toFloat()
        centerOfY = (height / 2).toFloat()
    }

    private fun createOpenValueAnimator(sector: SectorModel) =
        ValueAnimator.ofFloat(0f, increasingOffset)
            .apply {
                interpolator = LinearInterpolator()
                duration = animationDuration
                addUpdateListener { animator ->
                    sector.currentIncreasingDelta = animator.animatedValue as Float
                    invalidate()
                }
            }

    private fun startNecessaryAnimation(sector: SectorModel) {
        sector.apply {
            if (!isActive) {
                if (openValueAnimator?.isRunning == false) {
                    isActive = true
                    openValueAnimator?.start()
                }
            } else {
                isActive = false
                openValueAnimator?.reverse()
            }
        }
    }

    private fun findSectorByCoordinates(x: Float, y: Float): Int? {
        if (sectorAmount == 1) return 0
        val sectorNumber = findSector(x, y)

        if (x in centerOfX - radius - increasingOffset..centerOfX + radius + increasingOffset &&
            y in centerOfY - radius - increasingOffset..centerOfY + radius + increasingOffset &&
            sectors[sectorNumber].isActive
        ) return sectorNumber

        if (x in centerOfX - radius..centerOfX + radius &&
            y in centerOfY - radius..centerOfY + radius
        ) return sectorNumber

        return null
    }

    private fun findSector(x: Float, y: Float): Int {
        val angle = atan2(
            (height / 2) - y,
            (width / 2) - x
        ).toDegree() + 180
        return truncate(angle / sectorArc).toInt()
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
        if (sectorsInfo.size < sectorAmount) {
            throw NotEnoughInfoException(
                "Info.size = ${sectorsInfo.size} <->" + " SectorAmount = $sectorAmount"
            )
        } else {
            for (sectorNumber in 0 until sectorAmount) {
                sectors.add(createSector(sectorNumber, sectorsInfo[sectorNumber]))
            }
        }

        this@CircleView.sectors = sectors
        invalidate()
        setTouchListener()
    }

    private fun calculateIconCenterPoint(sector: SectorModel): Point {
        val x: Float
        val y: Float
        sector.apply {
            val rad = if (mutableRadius != 0f) mutableRadius else radius
            //This strange angle calculation cause of different system coordinates
            val iconAngleFromCircleCenterInDegree = 90 - startAngle - sweepAngle / 2
            val angleInRad = iconAngleFromCircleCenterInDegree.toRadian()
            val deltaX = (rad * iconOffsetByCenterInRadius * sin(angleInRad))
            val deltaY = (rad * iconOffsetByCenterInRadius * cos(angleInRad))

            x = (centerOfX * 2) + deltaX
            y = (centerOfY * 2) + deltaY
        }

        return Point(x, y)
    }

    private inner class Point(val x: Float, val y: Float)
    class NotEnoughInfoException(message: String) : Throwable(message)
    class IconOffsetByCenterInRadiusException(message: String) : Throwable(message)

    private inner class SectorModel(
        val startAngle: Float,
        val sweepAngle: Float,
        val sectorInfo: SectorInfo,
        var isActive: Boolean = false,
        var currentIncreasingDelta: Float = 0f,
        var mutableRadius: Float = 0f,
        var openValueAnimator: ValueAnimator? = null,
        var icon: Drawable? = null
    )

    private fun Float.toRadian(): Float = (this * PI / 180).toFloat()
    private fun Float.toDegree(): Float = (this / PI * 180).toFloat()
}