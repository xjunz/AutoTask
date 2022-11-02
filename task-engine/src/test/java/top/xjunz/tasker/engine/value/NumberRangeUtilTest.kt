package top.xjunz.tasker.engine.value

import org.junit.Test
import top.xjunz.tasker.engine.util.NumberRangeUtil


/**
 * @author xjunz 2022/11/01
 */
internal class NumberRangeUtilTest {

    @Test
    fun test() {
        assert(!NumberRangeUtil.contains(1, 2) { 3 })
        assert(NumberRangeUtil.contains(1.2F, 2.5F) { 2F })
        assert(NumberRangeUtil.contains(null, null) { 1 })
        assert(NumberRangeUtil.contains(null, 1F) { 0.5F })
        assert(NumberRangeUtil.contains(5L, null) { Long.MAX_VALUE })
    }

    @Test
    fun testLong() {
        assert(!NumberRangeUtil.contains(Long.MAX_VALUE - 2, Long.MAX_VALUE - 1) { Long.MAX_VALUE })
    }
}