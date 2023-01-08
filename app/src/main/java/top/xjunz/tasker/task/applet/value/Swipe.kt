/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

import androidx.test.uiautomator.Direction

/**
 * @author xjunz 2023/01/08
 */
class Swipe(val direction: Direction, val percent: Float, val speed: Int) {

    companion object {

        val ALL_DIRECTIONS = arrayOf(Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.DOWN)

        const val MAX_SPEED = 99999

        private val COMPOSER = BitwiseValueComposer.create(
            BitwiseValueComposer.bits(2),
            BitwiseValueComposer.float(1F),
            BitwiseValueComposer.integer(MAX_SPEED)
        )

        fun parse(composed: Long): Swipe {
            val parsed = COMPOSER.parse(composed)
            return Swipe(
                ALL_DIRECTIONS[parsed[0] as Int], parsed[1] as Float, parsed[2] as Int
            )
        }
    }

    fun compose(): Long {
        return COMPOSER.compose(ALL_DIRECTIONS.indexOf(direction), percent, speed)
    }
}