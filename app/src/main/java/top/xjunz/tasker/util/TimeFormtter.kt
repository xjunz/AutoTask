package top.xjunz.tasker.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * @author xjunz 2021/8/12
 */

private val dateFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
}

fun formatTime(timestamp: Long): String = dateFormat.format(Date(timestamp))

fun formatCurrentTime(): String = dateFormat.format(Date(System.currentTimeMillis()))