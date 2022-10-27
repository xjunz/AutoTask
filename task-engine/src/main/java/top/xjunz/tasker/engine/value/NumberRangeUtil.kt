package top.xjunz.tasker.engine.value

/**
 * @author xjunz 2022/10/23
 */
object NumberRangeUtil {

    inline fun contains(start: Number?, stop: Number?, element: () -> Number): Boolean {
        if (start == null && stop == null) return true
        val elementF = element().toFloat()
        return (start == null || elementF >= start.toFloat())
                && (stop == null || elementF <= stop.toFloat())
    }

}