/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.value

import kotlinx.serialization.Serializable

/**
 * The scoped distance used to match bounds.
 *
 * @author xjunz 2022/10/20
 */
@Serializable
data class Distance(
    var scope: Int = SCOPE_SCREEN,
    var unit: Int = UNIT_PX,
    var rangeStart: Float? = null,
    var rangeEnd: Float? = null
) {

    companion object {

        const val SCOPE_NONE = -1
        const val SCOPE_SCREEN = 0
        const val SCOPE_PARENT = 1

        const val UNIT_PX = 0
        const val UNIT_DP = 1
        const val UNIT_SCREEN_WIDTH = 2
        const val UNIT_SCREEN_HEIGHT = 3
        const val UNIT_PARENT_WIDTH = 4
        const val UNIT_PARENT_HEIGHT = 5

        fun unitToString(unit: Int): String {
            return when (unit) {
                UNIT_DP -> "dp"
                UNIT_PX -> "px"
                UNIT_SCREEN_WIDTH -> "sw"
                UNIT_SCREEN_HEIGHT -> "sh"
                UNIT_PARENT_WIDTH -> "pw"
                UNIT_PARENT_HEIGHT -> "ph"
                else -> "undefined"
            }
        }

        fun scopeToString(scope: Int): String {
            return when (scope) {
                SCOPE_NONE -> "none"
                SCOPE_SCREEN -> "screen"
                SCOPE_PARENT -> "parent"
                else -> "undefined"
            }
        }

        fun exactPx(px: Int): Distance {
            return Distance(SCOPE_NONE, UNIT_PX, px.toFloat(), px.toFloat())
        }

        fun exactPxInScreen(px: Int): Distance {
            return Distance(SCOPE_SCREEN, UNIT_PX, px.toFloat(), px.toFloat())
        }

        fun exactDpInParent(dp: Float): Distance {
            return Distance(SCOPE_PARENT, UNIT_DP, dp, dp)
        }
    }

    override fun toString(): String {
        return "Distance(scope=${scopeToString(scope)}," +
                " unit=${unitToString(unit)}," +
                " rangeStart=$rangeStart, rangeEnd=$rangeEnd)"
    }

}