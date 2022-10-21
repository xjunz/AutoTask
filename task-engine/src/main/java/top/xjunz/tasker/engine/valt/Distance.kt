package top.xjunz.tasker.engine.valt

import kotlinx.serialization.Serializable

/**
 * The scoped distance used to match bounds.
 *
 * @author xjunz 2022/10/20
 */
@Serializable
data class Distance(
    var scope: Int,
    var unit: Int,
    var portionScope: Int = SCOPE_NONE,
    var rangeStart: Float,
    var rangeEnd: Float
) {

    companion object {

        const val SCOPE_NONE = -1
        const val SCOPE_SCREEN = 0
        const val SCOPE_PARENT = 1

        const val UNIT_PX = 0
        const val UNIT_DP = 1
        const val UNIT_PORTION = 2

        const val NO_LIMIT = -1F

        fun unitToString(unit: Int): String {
            return when (unit) {
                UNIT_DP -> "dp"
                UNIT_PORTION -> "portion"
                UNIT_PX -> "px"
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

        fun exactPxInScreen(px: Int): Distance {
            return Distance(SCOPE_SCREEN, UNIT_PX, SCOPE_NONE, px.toFloat(), px.toFloat())
        }
    }

    override fun toString(): String {
        return "Distance(scope=${scopeToString(scope)}," +
                " unit=${unitToString(unit)}," +
                " portionScope=${scopeToString(portionScope)}," +
                " rangeStart=$rangeStart, rangeEnd=$rangeEnd)"
    }

}