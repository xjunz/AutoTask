/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

import top.xjunz.tasker.engine.applet.base.Applet

/**
 * @author xjunz 2023/01/13
 */
object VariantType {

    const val BITS_SWIPE = Applet.VAL_TYPE_LONG shl 8 or 2

    const val INT_COORDINATE = Applet.VAL_TYPE_INT shl 8 or 1

    const val TEXT_PACKAGE_NAME = Applet.VAL_TYPE_TEXT shl 8 or 1

    const val TEXT_ACTIVITY = Applet.VAL_TYPE_TEXT shl 8 or 2

    const val TEXT_APP_LIST = Applet.VAL_TYPE_TEXT shl 8 or 3

    const val TEXT_ACTIVITY_LIST = Applet.VAL_TYPE_TEXT shl 8 or 4

}