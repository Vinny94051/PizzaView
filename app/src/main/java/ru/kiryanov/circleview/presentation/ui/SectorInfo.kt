package ru.kiryanov.circleview.presentation.ui


import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

class SectorInfo(
    @ColorRes val color: Int = android.R.color.holo_blue_dark,
    @DrawableRes val drawableId : Int = android.R.drawable.alert_dark_frame
)