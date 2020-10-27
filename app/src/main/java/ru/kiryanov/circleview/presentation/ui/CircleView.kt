package ru.kiryanov.circleview.presentation.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
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


class CircleView(context: Context, @Nullable atrSet: AttributeSet) : View(context) {


    companion object {
        private const val SECTOR_OPEN_CLOSE_ANIMATION_DURATION = 650L
    }

    var paint: Paint = Paint()

    var centerOfX = 0f
    var centerOfY = 0f
    var radius = 0f
    var isCenter: Boolean = false
    var color1: Int? = null
    var color2: Int? = null
    var color3: Int? = null
    var color4: Int? = null
    var color5: Int? = null
    var color6: Int? = null
    var color7: Int? = null

    private var sectorAmount: Int
    private val sectors = mutableListOf<SectorModel>()
    private var clickListener: ((x: Float, y: Float) -> Unit)? = null
    private val defaultRectF = RectF()
    private val sectorsIcons = mutableListOf<Drawable>()

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
            isCenter = getBoolean(R.styleable.circleview_onCenter, false)
        }

        paint.initPaint(android.R.color.holo_blue_dark)

        color1 = android.R.color.holo_blue_dark
        color2 = android.R.color.darker_gray
        color3 = android.R.color.holo_blue_bright
        color4 = android.R.color.holo_red_dark
        color5 = android.R.color.holo_orange_dark
        color6 = android.R.color.holo_red_light
        color7 = android.R.color.holo_green_light

        setTouchListener()
    }

    private var currentSweepAngle: Int? = null
    private val arcAnimatorOn = ValueAnimator.ofInt(0, 100)
        .apply {
            duration = 650
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                currentSweepAngle = valueAnimator.animatedValue as Int
                invalidate()
            }
        }

    private val arcAnimationOff = ValueAnimator.ofInt(100, 0)
        .apply {
            duration = 650
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                currentSweepAngle = valueAnimator.animatedValue as Int
                invalidate()
            }
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


    //можно 1 а не 2
    private var counter = 0
    private var counter2 = 0

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
                        paint.changeColor(
                            when (index) {
                                0 -> color1!!
                                1 -> color2!!
                                2 -> color3!!
                                3 -> color4!!
                                4 -> color5!!
                                5 -> color6!!
                                else -> color7!!
                            }

                        )
                    )

                    sectorsIcons[sectorAmount - 1 - index].apply {
                        val delta = 4
                        bounds.left -= currentSweepAngle!! / delta
                        bounds.top -= currentSweepAngle!! / delta
                        bounds.right += currentSweepAngle!! / delta
                        bounds.bottom += currentSweepAngle!! / delta
                    }.draw(canvas!!)



                    if (isAnimationOn) arcAnimatorOn.start()
                    counter++
                    if (counter > 5) {
                        isAnimationOn = false
                        counter = 0
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
                            paint.changeColor(
                                when (index) {
                                    0 -> color1!!
                                    1 -> color2!!
                                    2 -> color3!!
                                    3 -> color4!!
                                    4 -> color5!!
                                    5 -> color6!!
                                    else -> color7!!
                                }
                            )
                        )

                        sectorsIcons[sectorAmount - 1 - index].apply {
                            val delta = 4
                            bounds.left += currentSweepAngle!! / delta
                            bounds.top += currentSweepAngle!! / delta
                            bounds.right -= currentSweepAngle!! / delta
                            bounds.bottom -= currentSweepAngle!! / delta
                        }.draw(canvas!!)

                        if (isAnimationOffOn) arcAnimationOff.start()
                        counter2++
                        if (counter2 > 5) {
                            isAnimationOffOn = false
                            counter2 = 0
                        }

                    } else {
//Default case

                        canvas?.drawArc(
                            coordinates, startAngle, sweepAngle, true,
                            paint.changeColor(
                                when (index) {
                                    0 -> color1!!
                                    1 -> color2!!
                                    2 -> color3!!
                                    3 -> color4!!
                                    4 -> color5!!
                                    5 -> color6!!
                                    else -> color7!!
                                }
                            )
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
        if (isCenter) {
            centerOfX = (width / 2).toFloat()
            centerOfY = (height / 2).toFloat()
        }
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

        for (sectorNumber in 0 until sectorAmount) {
            sectors.add(
                SectorModel(
                    sectorNumber, sectorNumber * sectorArc, sectorArc,
                    RectF(left, top, right, bottom)
                ).apply {
                    when (sectorAmount - 1 - sectorNumber) {
                        0 -> initImage(R.drawable.ic_baseline_account_circle_1, this)
                        1 -> initImage(R.drawable.ic_baseline_account_circle_2, this)
                        2 -> initImage(R.drawable.ic_baseline_account_circle_3, this)
                        3 -> initImage(R.drawable.ic_baseline_account_circle_4, this)
                        4 -> initImage(R.drawable.ic_baseline_account_circle_6, this)
                        5 -> initImage(R.drawable.ic_baseline_account_circle_5, this)
                    }

                }
            )
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
        }
    }

    private fun findSectorByCoordinates(x: Float, y: Float): Int? {
        val angle = atan2((height / 2) - y, (width / 2) - x).toDegree() + 180

        if (angle < sectors[1].startAngle) return 0

        for (index in 1..sectors.size - 2)
            if (angle > sectors[index - 1].startAngle && angle < sectors[index + 1].startAngle
            ) {
                return index
            }
        return sectors.size - 1
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initImage(@DrawableRes drawable: Int, sector: SectorModel) {
        val image: Drawable = resources.getDrawable(drawable, null)
        sector.coordinates.apply {

            val point = calculateImageLocation(sector)
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

    private fun calculateImageLocation(sector: SectorModel): Point {
        val x: Float
        val y: Float
        sector.apply {

            // 90f cause of coordinate system
            val imageAngleFromCircleCenterInDegree = (startAngle + (sweepAngle / 2) + 90f)
            val angleInRad = imageAngleFromCircleCenterInDegree.toRadian()
            val deltaX = (radius * sin(angleInRad))
            val deltaY = (radius * cos(angleInRad))

            x = (coordinates.centerX() * 2) + deltaX
            y = (coordinates.centerY() * 2) + deltaY

        }

        return Point(x, y)
    }


    private inner class Point(val x: Float, val y: Float)

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

    fun Float.toDp() = (this * Resources.getSystem().displayMetrics.density)

    fun Float.toPx() = (this / Resources.getSystem().displayMetrics.density)
}