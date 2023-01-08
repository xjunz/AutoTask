/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.value

import org.junit.Test
import top.xjunz.tasker.task.applet.util.NumberRangeUtil


/**
 * @author xjunz 2022/11/01
 */
internal class NumberRangeUtilTest {

    @Test
    fun test() {
        assert(!top.xjunz.tasker.task.applet.util.NumberRangeUtil.contains(1, 2) { 3 })
        assert(top.xjunz.tasker.task.applet.util.NumberRangeUtil.contains(1.2F, 2.5F) { 2F })
        assert(top.xjunz.tasker.task.applet.util.NumberRangeUtil.contains(null, null) { 1 })
        assert(top.xjunz.tasker.task.applet.util.NumberRangeUtil.contains(null, 1F) { 0.5F })
        assert(top.xjunz.tasker.task.applet.util.NumberRangeUtil.contains(5L, null) { Long.MAX_VALUE })
    }

    @Test
    fun testLong() {
        assert(!top.xjunz.tasker.task.applet.util.NumberRangeUtil.contains(Long.MAX_VALUE - 2, Long.MAX_VALUE - 1) { Long.MAX_VALUE })
    }
}