package ru.kiryanov.circleview.presentation.ui

import kotlin.math.PI


fun Float.toRadian(): Float = (this * PI / 180).toFloat()
fun Float.toDegree(): Float = (this / PI * 180).toFloat()