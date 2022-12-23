package top.xjunz.tasker.util

import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.str
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author xjunz 2021/8/12
 */

private val dateFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
}

fun Long.formatTime(): String = dateFormat.format(Date(this))

fun formatCurrentTime(): String = dateFormat.format(Date(System.currentTimeMillis()))

fun formatMinSecMills(mills: Int): String {
    val min = mills / (60 * 1000)
    val sec = mills % (60 * 1000) / 1000
    val mill = mills % 1000
    val sb = StringBuilder()
    if (min != 0) sb.append(min).append(R.string.minute.str)
    if (sec != 0) sb.append(sec).append(R.string.second.str)
    if (mill != 0) sb.append(mill).append(R.string.millisecond.str)
    return sb.toString()
}