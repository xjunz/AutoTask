/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.util

import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.str
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author xjunz 2021/8/12
 */

private val dateFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault())
}

fun Long.formatTime(): String = dateFormat.format(Date(this))

fun formatCurrentTime(): String = dateFormat.format(Date(System.currentTimeMillis()))

fun formatMinSecMills(mills: Number): String {
    val milliseconds = mills.toInt()
    val min = milliseconds / (60 * 1000)
    val sec = milliseconds % (60 * 1000) / 1000
    val mill = milliseconds % 1000
    val sb = StringBuilder()
    if (min != 0) sb.append(min).append(R.string.minute.str)
    if (sec != 0) sb.append(sec).append(R.string.second.str)
    if (mill != 0) sb.append(mill).append(R.string.millisecond.str)
    return sb.toString()
}