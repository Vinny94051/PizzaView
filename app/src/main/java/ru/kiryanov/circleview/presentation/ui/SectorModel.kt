package ru.kiryanov.circleview.presentation.ui

import android.graphics.RectF
import androidx.annotation.ColorRes

data class SectorModel(
    val sectorNumber: Int,
    val startAngle: Float,
    val sweepAngle : Float,
    var coordinates : RectF,
    var isActive : Boolean = false,
    var isAnimationOn : Boolean = false,
    var isAnimationOffOn : Boolean = false,
    @ColorRes val color: Int = android.R.color.holo_blue_dark,
    val data : Data = Data()
) {
    class Data()
}