/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.graphics.Point
import androidx.test.uiautomator.PointerGesture

/**
 * @author xjunz 2023/02/14
 */
class SamplingGesture(
    private val gesture: PointerGesture,
    private val sampleMills: Long
) {

    companion object {

        private const val SEPARATOR = ';'

        fun inflateFromString(flattened: String): SamplingGesture {
            val split = flattened.split(SEPARATOR)
            check(split.size > 1)
            val startDelay = split[0].toLong()
            val sampleMills = split[1].toLong()
            var gesture: PointerGesture? = null
            for (i in 1..split.lastIndex) {
                val element = split[i]
                if (element.isEmpty()) continue
                val composed = element.toLong(16)
                val repeat = composed ushr 32
                check(repeat >= 0)
                val x = composed ushr 16 and 0xFFFF
                val y = composed and 0xFFFF
                if (gesture == null) {
                    check(repeat == 0L)
                    gesture = PointerGesture(Point(x.toInt(), y.toInt()), startDelay)
                }
                if (repeat > 0) {
                    gesture.pause((repeat + 1) * sampleMills)
                } else {
                    gesture.moveWithDuration(Point(x.toInt(), y.toInt()), sampleMills)
                }
            }
            return SamplingGesture(gesture!!, sampleMills)
        }
    }

    private fun Point.flatten(repeat: Long): String {
        return (x.toLong() shl 16 or y.toLong() or (repeat shl 32)).toString(16)
    }

    override fun toString(): String {
        var start: Point? = null
        val sb = StringBuilder()
        sb.append(gesture.delay()).append(SEPARATOR)
        sb.append(sampleMills).append(SEPARATOR)
        gesture.actions.forEach {
            if (start == null) {
                start = it.start
                sb.append(it.start.flatten(0))
            } else {
                sb.append(it.end.flatten(it.duration / sampleMills - 1))
            }.append(SEPARATOR)
        }
        return sb.toString()
    }
}