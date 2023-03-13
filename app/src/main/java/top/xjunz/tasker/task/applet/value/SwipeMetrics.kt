/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

import androidx.test.uiautomator.Direction

/**
 * @author xjunz 2023/01/08
 */
class SwipeMetrics(val direction: Direction, val percent: Float, val speed: Int) {

    companion object {

        private const val MAX_SPEED = 99999

        val COMPOSER = BitwiseValueComposer.create(
            BitwiseValueComposer.bits(2),
            BitwiseValueComposer.percent(),
            BitwiseValueComposer.integer(MAX_SPEED)
        )

        fun parse(composed: Long): SwipeMetrics {
            val parsed = COMPOSER.parse(composed)
            return SwipeMetrics(
                Direction.ALL_DIRECTIONS[parsed[0] as Int],
                (parsed[1] as Int) / 100F,
                parsed[2] as Int
            )
        }
    }

    fun compose(): Long {
        return COMPOSER.compose(Direction.ALL_DIRECTIONS.indexOf(direction), percent * 100, speed)
    }
}