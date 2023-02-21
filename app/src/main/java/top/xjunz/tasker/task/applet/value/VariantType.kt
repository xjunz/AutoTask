/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

import top.xjunz.tasker.engine.applet.base.Applet

/**
 * @author xjunz 2023/01/13
 */
object VariantType {

    const val BITS_SWIPE = 1 shl 16 or Applet.VAL_TYPE_LONG

    const val INT_COORDINATE = 1 shl 16 or Applet.VAL_TYPE_INT

    const val INT_INTERVAL = 2 shl 16 or Applet.VAL_TYPE_INT

    const val INT_INTERVAL_XY = 3 shl 16 or Applet.VAL_TYPE_INT

    const val TEXT_PACKAGE_NAME = 1 shl 16 or Applet.VAL_TYPE_TEXT

    const val TEXT_ACTIVITY = 2 shl 16 or Applet.VAL_TYPE_TEXT

    const val TEXT_PANE_TITLE = 3 shl 16 or Applet.VAL_TYPE_TEXT

    const val TEXT_GESTURES = 4 shl 16 or Applet.VAL_TYPE_TEXT
}