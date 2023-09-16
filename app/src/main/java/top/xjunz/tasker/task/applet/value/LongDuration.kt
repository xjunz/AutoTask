/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

/**
 * @author xjunz 2023/09/11
 */
class LongDuration(val day: Int, val hour: Int, val min: Int, val sec: Int) {

    companion object {

        val COMPOSER = BitwiseValueComposer.create(
            BitwiseValueComposer.integer(31),
            BitwiseValueComposer.integer(59),
            BitwiseValueComposer.integer(59),
            BitwiseValueComposer.integer(59)
        )

        fun parse(composed: Long): LongDuration {
            val parsed = COMPOSER.parse(composed)
            return LongDuration(
                parsed[0] as Int,
                parsed[1] as Int,
                parsed[2] as Int,
                parsed[3] as Int
            )
        }
    }

    fun toMilliseconds(): Long {
        return day * (1000 * 60 * 60 * 24) + hour * (1000 * 60 * 60) + min * (1000 * 60) + sec * 1000L
    }
}