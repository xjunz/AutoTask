/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

import androidx.test.uiautomator.Direction

/**
 * @author xjunz 2023/03/13
 */
class ScrollMetrics(val direction: Direction, val speed: Int) {

    companion object {

        val COMPOSER = BitwiseValueComposer.create(
            BitwiseValueComposer.integer(3),
            BitwiseValueComposer.integer(100)
        )

        fun parse(composed: Long): ScrollMetrics {
            val parsed = COMPOSER.parse(composed)
            return ScrollMetrics(Direction.ALL_DIRECTIONS[parsed[0] as Int], parsed[1] as Int)
        }
    }

    val steps: Int
        get() {
            check(speed in 1..99) {
                "Speed must in 1..99"
            }
            return 100 - speed
        }

    val isVertical: Boolean get() = direction == Direction.DOWN || direction == Direction.UP
}