package ru.kiryanov.circleview.presentation.ui


import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

class SectorInfo(
    @ColorRes val closeColor: Int = android.R.color.holo_blue_dark,
    @ColorRes val openColor : Int = android.R.color.holo_green_light,
    @DrawableRes val drawableId : Int = android.R.drawable.alert_dark_frame
)