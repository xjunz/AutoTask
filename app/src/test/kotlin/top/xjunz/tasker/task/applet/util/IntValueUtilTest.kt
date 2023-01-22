/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.util

import org.junit.Test


/**
 * @author xjunz 2023/01/22
 */
internal class IntValueUtilTest {

    @Test
    fun parseTime() {
    }

    @Test
    fun composeTime() {
        println(IntValueUtil.parseTime(IntValueUtil.composeTime(23,59,59)).joinToString())
    }

    @Test
    fun parseCoordinate() {
    }

    @Test
    fun composeCoordinate() {
    }
}