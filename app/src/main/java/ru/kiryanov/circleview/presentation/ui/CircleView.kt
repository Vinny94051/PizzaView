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
import android.util.Log
import android.util.TimeUtils
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import ru.kiryanov.circleview.R
import java.util.concurrent.TimeUnit
import kotlin.math.*


class CircleView(context: Context, @Nullable atrSet: AttributeSet) : View(context, atrSet) {

    companion object {
        private const val SECTOR_OPEN_CLOSE_ANIMATION_DURATION = 650L
        private const val MIN_HOLDING_SECTOR_TIME_IN_MILLS = 40
        private const val MAX_HOLDING_SECTOR_TIME_IN_MILLS = 1000
    }

    var paint: Paint = Paint()
    var centerOfX = 0f
    var centerOfY = 0f
    var radius = 0f

    private var counterOpenAnimation = 0
    private var counterCloseAnimation = 0
    private var sectorAmount: Int
    private val sectors = mutableListOf<SectorModel>()
    private var clickListener: ((x: Float, y: Float) -> Unit)? = null
    private val defaultRectF = RectF()
    private var openedRectF = RectF()
    private val sectorsIcons = mutableListOf<Drawable>()
    private val sectorIconsOffsets = mutableListOf<Rect>()
    private var isOpenedRectFUpdated = false
    var sectorsInfo: List<SectorModel.Data>? = null
        set(value) {
            field = value
            invalidate()
        }

    private val sectorArc: Float
        get() {
            return 360f.div(sectorAmount.toFloat())
        }


    init {
        Log.e(javaClass.simpleName, "init")
        context.theme.obtainStyledAttributes(
            atrSet,
            R.styleable.circleview, 0, 0
        ).apply {
            sectorAmount = getInteger(R.styleable.circleview_sector_amount, 1)
            radius = getDimension(R.styleable.circleview_circle_radius, 0f)
        }
        paint.initPaint(android.R.color.holo_blue_dark)
        setTouchListener()
    }

    private var currentSweepAngle: Int? = null
    private val arcAnimatorOn = ValueAnimator.ofInt(0, 100)
        .apply {
            duration = SECTOR_OPEN_CLOSE_ANIMATION_DURATION
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                currentSweepAngle = valueAnimator.animatedValue as Int
                invalidate()
            }
        }

    private val arcAnimationOff = ValueAnimator.ofInt(100, 0)
        .apply {
            duration = SECTOR_OPEN_CLOSE_ANIMATION_DURATION
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                Log.d(javaClass.simpleName, "${valueAnimator.animatedValue as Int}")
                currentSweepAngle = valueAnimator.animatedValue as Int
                invalidate()
            }
        }

    //TODO right on Measure calculation
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension((radius * 3).toInt(), (radius * 3).toInt())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        initSectors()
        clickListener = { x, y ->
            val secNum = findSectorByCoordinates(x, y)
            secNum?.let { num ->
                resetSectorAnimationFlags(sectors[num])
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        sectors.forEachIndexed { index, sector ->
            sector.apply {

                if (isActive && isAnimationOn && !isAnimationOffOn) {
                    //Open sector by tap
                    currentSweepAngle = 20
                    canvas?.drawArc(
                        coordinates.apply {
                            val delta = currentSweepAngle?.toFloat() ?: 0f
                            top -= delta
                            bottom += delta
                            right += delta
                            left -= delta
                        },
                        startAngle,
                        sweepAngle,
                        true,
                        paint.changeColor(data.color)
                    )

                    sectorsIcons[index].apply {
                        val delta = 4


                        when (startAngle + sweepAngle / 2) {
                            in 0f..90f -> {
                                bounds.left += currentSweepAngle!! / delta
                                bounds.top += currentSweepAngle!! / delta
                                bounds.right += currentSweepAngle!! / delta
                                bounds.bottom += currentSweepAngle!! / delta
                            }

                            in 90.1f..180f -> {
                                bounds.left -= currentSweepAngle!! / delta
                                bounds.top += currentSweepAngle!! / delta
                                bounds.right -= currentSweepAngle!! / delta
                                bounds.bottom += currentSweepAngle!! / delta
                            }

                            in 180.1f..270f -> {
                                bounds.left -= currentSweepAngle!! / delta
                                bounds.top -= currentSweepAngle!! / delta
                                bounds.right -= currentSweepAngle!! / delta
                                bounds.bottom -= currentSweepAngle!! / delta
                            }

                            else -> {
                                bounds.left += currentSweepAngle!! / delta
                                bounds.top -= currentSweepAngle!! / delta
                                bounds.right += currentSweepAngle!! / delta
                                bounds.bottom -= currentSweepAngle!! / delta
                            }
                        }
                    }.draw(canvas!!)



                    if (isAnimationOn) arcAnimatorOn.start()
                    counterOpenAnimation++
                    if (counterOpenAnimation > 5) {
                        //save open rect coordinates
                        if (!isOpenedRectFUpdated) {
                            openedRectF = coordinates.copy()
                            isOpenedRectFUpdated = true
                        }
                        isAnimationOn = false
                        counterOpenAnimation = 0
                    }

                } else {

                    if (coordinates.left < defaultRectF.left && !isActive) {
                        //Close activated sector
                        Log.d(javaClass.simpleName, "Close")
                        currentSweepAngle = 20
                        canvas?.drawArc(
                            coordinates.apply {
                                val delta = currentSweepAngle?.toFloat() ?: 0f
                                top += delta
                                bottom -= delta
                                right -= delta
                                left += delta
                            },
                            startAngle,
                            sweepAngle,
                            true,
                            paint.changeColor(data.color)
                        )



                        sectorsIcons[index].apply {
                            val delta = 4
                            when (startAngle + sweepAngle / 2) {
                                in 0f..90f -> {
                                    bounds.left -= currentSweepAngle!! / delta
                                    bounds.top -= currentSweepAngle!! / delta
                                    bounds.right -= currentSweepAngle!! / delta
                                    bounds.bottom -= currentSweepAngle!! / delta
                                }

                                in 90.1f..180f -> {
                                    bounds.left += currentSweepAngle!! / delta
                                    bounds.top -= currentSweepAngle!! / delta
                                    bounds.right += currentSweepAngle!! / delta
                                    bounds.bottom -= currentSweepAngle!! / delta
                                }

                                in 180.1f..270f -> {
                                    bounds.left += currentSweepAngle!! / delta
                                    bounds.top += currentSweepAngle!! / delta
                                    bounds.right += currentSweepAngle!! / delta
                                    bounds.bottom += currentSweepAngle!! / delta
                                }

                                else -> {
                                    bounds.left -= currentSweepAngle!! / delta
                                    bounds.top += currentSweepAngle!! / delta
                                    bounds.right -= currentSweepAngle!! / delta
                                    bounds.bottom += currentSweepAngle!! / delta
                                }
                            }
                        }.draw(canvas!!)

                        if (isAnimationOffOn) arcAnimationOff.start()
                        counterCloseAnimation++
                        if (counterCloseAnimation > 5) {
                            isAnimationOffOn = false
                            counterCloseAnimation = 0
                        }

                    } else {
                        //Default case
                        Log.d(javaClass.simpleName, "default case")
                        canvas?.drawArc(
                            coordinates, startAngle, sweepAngle, true,
                            paint.changeColor(data.color)
                        )
                    }
                    canvas?.let { canva ->
                        sectorsIcons.forEach {
                            it.draw(canva)
                        }
                    }
                }
            }
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
                if (isRightAction()) {
                    clickListener?.invoke(eventX, eventY)
                }

                return@setOnTouchListener true
            }

            false
        }
    }

    private fun isRightAction() =
        (actionUpClickTime - actionDownClickTime) in
                MIN_HOLDING_SECTOR_TIME_IN_MILLS..MAX_HOLDING_SECTOR_TIME_IN_MILLS


    private fun initSectors() {
        centerOfX = (width / 2).toFloat()
        centerOfY = (height / 2).toFloat()
        val left = centerOfX - radius
        val right = centerOfX + radius
        val top = centerOfY - radius
        val bottom = centerOfY + radius

        defaultRectF.apply {
            this.left = left
            this.right = right
            this.top = top
            this.bottom = bottom
        }

        if (sectors.size != sectorAmount && sectorsIcons.size != sectorAmount) {
            sectorsInfo?.let { info ->
                if (info.size < sectorAmount) {
                    throw NotEnoughInfoException("Info.size = ${info.size} <-> SectorAmount = $sectorAmount")
                } else {
                    for (sectorNumber in 0 until sectorAmount) {
                        sectors.add(
                            SectorModel(
                                sectorNumber, sectorNumber * sectorArc, sectorArc,
                                RectF(left, top, right, bottom), data = info[sectorNumber]
                            ).apply {
                                initImage(this.data.drawable, this)
                                sectorIconsOffsets.add(
                                    calculateOffsetImageBounds(
                                        this.data.drawable,
                                        calculateImageCenterPoint(this)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun resetSectorAnimationFlags(sector: SectorModel) {
        if (!sector.isActive) {
            sector.isActive = true
            sector.isAnimationOn = true
            invalidate()
        } else {
            sector.isActive = false
            sector.isAnimationOffOn = true
            invalidate()
        }
    }

    private fun findSectorByCoordinates(x: Float, y: Float): Int? {

        if (x in openedRectF.left..openedRectF.right &&
            y in openedRectF.top..openedRectF.bottom
        ) {
            val sectorNumber = findSector(x, y)
            if (sectors[sectorNumber].isActive) return sectorNumber
        }


        if (x in defaultRectF.left..defaultRectF.right &&
            y in defaultRectF.top..defaultRectF.bottom
        ) {
            return findSector(x, y)
        }
        return null
    }

    private fun findSector(x: Float, y: Float): Int {
        val angle = atan2(
            (height / 2) - y,
            (width / 2) - x
        ).toDegree() + 180

        if (angle in 0f..sectors[1].startAngle) return 0

        for (index in 1..sectors.size - 2) {
            if (angle in sectors[index - 1].startAngle..sectors[index + 1].startAngle) {
                return index
            }
        }
        return sectors.size - 1
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initImage(@DrawableRes drawable: Int, sector: SectorModel) {
        val image: Drawable = resources.getDrawable(drawable, null)
        sector.coordinates.apply {

            val point = calculateImageCenterPoint(sector)
            val imageCenterX = point.x
            val imageCenterY = point.y
            val delta = 2

            image.setBounds(
                (imageCenterX - image.minimumWidth).toInt() / delta,
                (imageCenterY - image.minimumHeight).toInt() / delta,
                (imageCenterX + image.minimumWidth).toInt() / delta,
                (imageCenterY + image.minimumHeight).toInt() / delta
            )

        }
        sectorsIcons.add(image)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun calculateOffsetImageBounds(@DrawableRes drawable: Int, point: Point): Rect {
        val delta = 2
        val imageCenterX = point.x
        val imageCenterY = point.y
        val image: Drawable = resources.getDrawable(drawable, null)

        return Rect(
            (imageCenterX - image.minimumWidth).toInt() / delta,
            (imageCenterY - image.minimumHeight).toInt() / delta,
            (imageCenterX + image.minimumWidth).toInt() / delta,
            (imageCenterY + image.minimumHeight).toInt() / delta
        )
    }

    private fun calculateImageCenterPoint(sector: SectorModel): Point {
        val x: Float
        val y: Float
        sector.apply {

            //This strange angle calculation cause of different system coordinates
            val imageAngleFromCircleCenterInDegree = -(startAngle + (sweepAngle / 2) + 90) + 180
            val angleInRad = imageAngleFromCircleCenterInDegree.toRadian()
            val deltaX = (radius * 1.5f * sin(angleInRad))
            val deltaY = (radius * 1.5f * cos(angleInRad))

            x = (coordinates.centerX() * 2) + deltaX
            y = (coordinates.centerY() * 2) + deltaY

        }

        return Point(x, y)
    }

    private fun calculateImageOffsetPoint(sector: SectorModel): Point {
        val centerPoint = calculateImageCenterPoint(sector)

        val centerX = centerPoint.x
        val centerY = centerPoint.y

        val alpha = (sector.sweepAngle / 2).toRadian()

        when (sector.startAngle + sector.sweepAngle / 2) {
            in 0f..90f -> {
                return Point(
                    (radius * sin(alpha) + 3 * centerX) / 3,
                    (radius * cos(alpha) + 3 * centerY) / 3
                )
            }

            in 90.1f..180f -> {
                return Point(
                    (-radius * sin(alpha) - 3 * centerX) / 3,
                    (radius * cos(alpha) + 3 * centerY) / 3
                )
            }

            in 180.1f..270f -> {
                return Point(
                    (-radius * sin(alpha) - 3 * centerX) / 3,
                    (-radius * cos(alpha) - 3 * centerY) / 3
                )
            }

            else -> {
                return Point(
                    (radius * sin(alpha) + 3 * centerX) / 3,
                    (radius * cos(alpha) + 3 * centerY) / 3
                )
            }
        }
    }

    private inner class Point(val x: Float, val y: Float)
    class NotEnoughInfoException(message: String) : Throwable(message)

    private fun Paint.initPaint(@ColorRes colorId: Int) =
        this.apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, colorId)
        }

    private fun Paint.changeColor(@ColorRes colorId: Int) =
        this.apply {
            color = ContextCompat.getColor(context, colorId)
        }

    private fun RectF.copy() =
        RectF(
            this.left,
            this.top,
            this.right,
            this.bottom
        )

    private fun Float.toRadian(): Float = (this * PI / 180).toFloat()
    private fun Float.toDegree(): Float = (this / PI * 180).toFloat()
}