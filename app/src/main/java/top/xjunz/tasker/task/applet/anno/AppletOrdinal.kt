/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.anno

import androidx.annotation.Keep

/**
 * |0000 0000|│|0000 0000|
 * |  :----: |:----: | :----:  |
 * |id|│|index|
 *
 * The first 8 bits of [ordinal] is the category id and the last 8 bits of [ordinal] is
 * the index in the category.
 *
 * @author xjunz 2022/09/22
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
@Keep
annotation class AppletOrdinal(val ordinal: Int)