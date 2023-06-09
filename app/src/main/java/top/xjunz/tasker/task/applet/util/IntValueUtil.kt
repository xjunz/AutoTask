/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.util

import android.graphics.Point

/**
 * @author xjunz 2023/01/08
 */
object IntValueUtil {

    private fun integerArrayOf(vararg ints: Int): Array<Int> {
        return ints.toTypedArray()
    }

    fun parseTime(time: Int): Array<Int> {
        return integerArrayOf(time ushr 16 and 0xFF, time ushr 8 and 0xFF, time and 0xFF)
    }

    fun composeTime(hour: Int, min: Int, sec: Int): Int {
        return hour shl 16 or (min shl 8) or sec
    }

    fun parseXY(coordinate: Int): Point {
        return Point(coordinate ushr 16, coordinate and 0xFFFF)
    }

    fun composeXY(x: Int, y: Int): Int {
        check(x in 0..0xFFFF)
        check(y in 0..0xFFFF)
        return x shl 16 or y
    }
}