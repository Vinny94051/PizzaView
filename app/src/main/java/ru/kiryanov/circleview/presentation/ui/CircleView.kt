package ru.kiryanov.circleview.presentation.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import ru.kiryanov.circleview.R
import kotlin.math.*


class CircleView(context: Context, @Nullable atrSet: AttributeSet) : View(context, atrSet) {

    companion object {
        private const val SECTOR_OPEN_CLOSE_ANIMATION_DURATION = 650L
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
    private val sectorsIcons = mutableListOf<Drawable>()
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
                animateSector(sectors[num])
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
                        bounds.left -= currentSweepAngle!! / delta
                        bounds.top -= currentSweepAngle!! / delta
                        bounds.right += currentSweepAngle!! / delta
                        bounds.bottom += currentSweepAngle!! / delta
                    }.draw(canvas!!)



                    if (isAnimationOn) arcAnimatorOn.start()
                    counterOpenAnimation++
                    if (counterOpenAnimation > 5) {
                        isAnimationOn = false
                        counterOpenAnimation = 0
                    }

                } else {

                    if (coordinates.left < defaultRectF.left && !isActive) {

//Close activated sector
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
                            bounds.left += currentSweepAngle!! / delta
                            bounds.top += currentSweepAngle!! / delta
                            bounds.right -= currentSweepAngle!! / delta
                            bounds.bottom -= currentSweepAngle!! / delta
                        }.draw(canvas!!)

                        if (isAnimationOffOn) arcAnimationOff.start()
                        counterCloseAnimation++
                        if (counterCloseAnimation > 5) {
                            isAnimationOffOn = false
                            counterCloseAnimation = 0
                        }

                    } else {
//Default case

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

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener() {
        this.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                clickListener?.invoke(event.x, event.y)
                return@setOnTouchListener true
            }

            false
        }
    }

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
                            }
                        )
                    }
                }
            }
        }
    }

    private fun animateSector(sector: SectorModel) {
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
        if (x in defaultRectF.left..defaultRectF.right &&
            y in defaultRectF.top..defaultRectF.bottom
        ) {
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
        return null
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

    private fun Float.toRadian(): Float = (this * PI / 180).toFloat()
    private fun Float.toDegree(): Float = (this / PI * 180).toFloat()
}