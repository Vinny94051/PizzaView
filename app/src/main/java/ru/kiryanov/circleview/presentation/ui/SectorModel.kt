package ru.kiryanov.circleview.presentation.ui

import android.animation.ValueAnimator
import android.graphics.RectF
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class SectorModel(
    val sectorNumber: Int,
    val startAngle: Float,
    val sweepAngle : Float,
    var coordinates : RectF,
    var isActive  : Boolean = false,
    var isAnimationOn  : Boolean = false,
    var currentIncreasingDelta : Float = 0f,
    var mutableRadius : Float = 0f,
    var openValueAnimator : ValueAnimator? = null,
    var closeValueAnimator: ValueAnimator? = null,
    val data : Data
){
     class Data(
        @ColorRes val color: Int = android.R.color.holo_blue_dark,
        @DrawableRes val drawable : Int = android.R.drawable.alert_dark_frame
    )
}