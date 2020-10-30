package ru.kiryanov.circleview.presentation.ui

import android.animation.Animator
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
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import ru.kiryanov.circleview.R
import kotlin.math.*


class CircleView(context: Context, @Nullable atrSet: AttributeSet) : View(context, atrSet) {

    companion object {
        private const val MIN_HOLDING_SECTOR_TIME_IN_MILLS = 40
        private const val MAX_HOLDING_SECTOR_TIME_IN_MILLS = 1000
    }

    var paint: Paint = Paint()
    var centerOfX = 0f
    var centerOfY = 0f
    var radius = 0f
    var increaseCoefficient: Float = 0f

    private var sectorAmount: Int
    private val sectors = mutableListOf<SectorModel>()
    private var clickListener: ((x: Float, y: Float) -> Unit)? = null
    private val defaultRectF = RectF()
    private var openedRectF = RectF()
    private val sectorsIcons = mutableListOf<Drawable>()
    private val sectorIconsOffsets = mutableListOf<Rect>()

    val animationDuration: Long
    var sectorsInfo: List<Data>? = null
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
            animationDuration = getFloat(R.styleable.circleview_animation_duration, 200f).toLong()
            increaseCoefficient = getDimension(R.styleable.circleview_increasing_offset, radius / 3)
        }
        paint.initPaint(android.R.color.holo_blue_dark)
        setTouchListener()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val viewSideLength = ((increaseCoefficient + radius) * 2).toInt()
        setMeasuredDimension(viewSideLength, viewSideLength)
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
                if (isActive && isAnimationOn) {
                    //Open sector by tap
                    animateSector(this, index, canvas)
                    if (openValueAnimator?.isRunning == false) isAnimationOn = false
                } else {
                    if (!isActive && isAnimationOn) {
                        //Close activated sector
                        animateSector(this, index, canvas)
                        if (closeValueAnimator?.isRunning == false) isAnimationOn = true
                    } else {
                        //Default case
                        with(canvas!!) {
                            drawArc(coordinates, startAngle, sweepAngle, true,
                                paint.changeColor(data.color))
                            sectorsIcons.forEach { drawable ->
                                drawable.draw(this)
                            }
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
                if (isClickAction()) {
                    clickListener?.invoke(eventX, eventY)
                }

                return@setOnTouchListener true
            }

            false
        }
    }

    private fun animateSector(sector: SectorModel, sectorIndex: Int, canvas: Canvas?) {
        with(sector) {
            canvas?.drawArc(
                coordinates.apply {
                    top = defaultRectF.top - sector.currentIncreasingDelta
                    bottom = defaultRectF.bottom + sector.currentIncreasingDelta
                    right = defaultRectF.right + sector.currentIncreasingDelta
                    left = defaultRectF.left - sector.currentIncreasingDelta
                },
                startAngle,
                sweepAngle,
                true,
                paint.changeColor(data.color)
            )

            sectorsIcons[sectorIndex].apply {
                mutableRadius = radius + sector.currentIncreasingDelta
                bounds = calculateOffsetImageBounds(
                    this,
                    calculateImageCenterPoint(sector)
                )
            }.draw(canvas!!)
        }
    }

    private fun isClickAction() =
        (actionUpClickTime - actionDownClickTime) in
                MIN_HOLDING_SECTOR_TIME_IN_MILLS..MAX_HOLDING_SECTOR_TIME_IN_MILLS


    private fun initSectors() {
        initCenterPointAndBounds()
        if (sectors.size != sectorAmount && sectorsIcons.size != sectorAmount) {
            sectorsInfo?.let { info ->
                if (info.size < sectorAmount) {
                    throw NotEnoughInfoException(
                        "Info.size = ${info.size} <->" +
                                " SectorAmount = $sectorAmount"
                    )
                } else {
                    for (sectorNumber in 0 until sectorAmount) {
                        sectors.add(createSector(sectorNumber, info[sectorNumber]))
                    }
                }
            }
        }
    }

    private fun createSector(sectorNumber: Int, data: Data): SectorModel {
        return SectorModel(
            sectorNumber, sectorNumber * sectorArc, sectorArc,
            RectF(
                defaultRectF.left,
                defaultRectF.top,
                defaultRectF.right,
                defaultRectF.bottom
            ),
            mutableRadius = radius,
            data = data
        ).apply {
            initImage(this.data.drawable, this)
            sectorIconsOffsets.add(
                calculateOffsetImageBounds(
                    ResourcesCompat.getDrawable(
                        resources,
                        this.data.drawable,
                        null
                    )!!,
                    calculateImageCenterPoint(this)
                )
            )
            openValueAnimator = createOpenValueAnimator(this)
            closeValueAnimator = createCloseValueAnimator(this)
        }
    }

    private fun initCenterPointAndBounds() {
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

        openedRectF.apply {
            this.left = defaultRectF.left - increaseCoefficient
            this.right = defaultRectF.right + increaseCoefficient
            this.top = defaultRectF.top - increaseCoefficient
            this.bottom = defaultRectF.bottom + increaseCoefficient
        }
    }

    private fun createOpenValueAnimator(sector: SectorModel) =
        ValueAnimator.ofFloat(0f, increaseCoefficient)
            .apply {
                interpolator = LinearInterpolator()
                duration = animationDuration
                addUpdateListener { animator ->
                    sector.currentIncreasingDelta = animator.animatedValue as Float
                    invalidate()
                }
                setOnAnimationStartListener {
                    sector.isAnimationOn = true
                }
            }

    private fun createCloseValueAnimator(sector: SectorModel) =
        ValueAnimator.ofFloat(increaseCoefficient, 0f)
            .apply {
                interpolator = LinearInterpolator()
                duration = animationDuration
                addUpdateListener { animator ->
                    sector.currentIncreasingDelta = animator.animatedValue as Float
                    Log.d(javaClass.simpleName, "${animator.animatedValue as Float}")
                    invalidate()
                }
                setOnAnimationStartListener {
                    sector.isAnimationOn = true
                }
            }

    private fun resetSectorAnimationFlags(sector: SectorModel) {
        sector.apply {
            if (!isActive) {
                isActive = true
                closeValueAnimator?.cancel()
                openValueAnimator?.start()
            } else {
                isActive = false
                openValueAnimator?.cancel()
                closeValueAnimator?.start()
            }
        }
    }

    private fun findSectorByCoordinates(x: Float, y: Float): Int? {

        if (sectorAmount == 1) return 0

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
            if (angle in sectors[index].startAngle..sectors[index + 1].startAngle) {
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
    private fun calculateOffsetImageBounds(image: Drawable, point: Point): Rect {
        val delta = 2
        val imageCenterX = point.x
        val imageCenterY = point.y

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
            val rad = if (mutableRadius != 0f) mutableRadius else radius
            //This strange angle calculation cause of different system coordinates
            val imageAngleFromCircleCenterInDegree = 90 - startAngle - sweepAngle / 2
            val angleInRad = imageAngleFromCircleCenterInDegree.toRadian()
            val deltaX = (rad * 1.5f * sin(angleInRad))
            val deltaY = (rad * 1.5f * cos(angleInRad))

            x = (coordinates.centerX() * 2) + deltaX
            y = (coordinates.centerY() * 2) + deltaY

        }

        return Point(x, y)
    }

    private inner class Point(val x: Float, val y: Float)
    class NotEnoughInfoException(message: String) : Throwable(message)

    private inner class SectorModel(
        val sectorNumber: Int,
        val startAngle: Float,
        val sweepAngle: Float,
        var coordinates: RectF,
        var isActive: Boolean = false,
        var isAnimationOn: Boolean = false,
        var currentIncreasingDelta: Float = 0f,
        var mutableRadius: Float = 0f,
        var openValueAnimator: ValueAnimator? = null,
        var closeValueAnimator: ValueAnimator? = null,
        val data: Data
    )

    private fun Paint.initPaint(@ColorRes colorId: Int) =
        this.apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, colorId)
        }

    private fun Paint.changeColor(@ColorRes colorId: Int) =
        this.apply {
            color = ContextCompat.getColor(context, colorId)
        }

    private fun ValueAnimator.setOnAnimationStartListener(action: () -> Unit) {
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {
                action.invoke()
            }
        })
    }

    private fun Float.toRadian(): Float = (this * PI / 180).toFloat()
    private fun Float.toDegree(): Float = (this / PI * 180).toFloat()
}