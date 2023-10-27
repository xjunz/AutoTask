/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

import top.xjunz.tasker.engine.applet.base.Applet

/**
 * @author xjunz 2023/01/13
 */
object VariantArgType {

    /**
     * A bit mask indicating that a variant type is not responsible for type matching.
     */
    private const val MASK_IGNORE_VARIANT_TYPE_WHEN_MATCHING = 1 shl 24

    const val NONE = -1

    const val BITS_SWIPE = 1 shl 16 or Applet.ARG_TYPE_LONG

    const val BITS_SCROLL = 2 shl 16 or Applet.ARG_TYPE_LONG

    const val BITS_LONG_DURATION = 3 shl 16 or Applet.ARG_TYPE_LONG

    const val LONG_TIME = 4 shl 16 or Applet.ARG_TYPE_LONG

    const val BITS_BOUNDS = 5 shl 16 or Applet.ARG_TYPE_LONG

    const val INT_COORDINATE = 1 shl 16 or Applet.ARG_TYPE_INT

    const val INT_INTERVAL = 2 shl 16 or Applet.ARG_TYPE_INT

    const val INT_INTERVAL_XY = 3 shl 16 or Applet.ARG_TYPE_INT

    const val INT_QUANTITY = 4 shl 16 or Applet.ARG_TYPE_INT

    const val INT_ROTATION = 5 shl 16 or Applet.ARG_TYPE_INT

    const val INT_TIME_OF_DAY = 6 shl 16 or Applet.ARG_TYPE_INT

    const val INT_MONTH = 7 shl 16 or Applet.ARG_TYPE_INT

    const val INT_DAY_OF_MONTH = 8 shl 16 or Applet.ARG_TYPE_INT

    const val INT_DAY_OF_WEEK = 9 shl 16 or Applet.ARG_TYPE_INT

    const val INT_HOUR_OF_DAY = 10 shl 16 or Applet.ARG_TYPE_INT

    const val INT_MIN_OR_SEC = 11 shl 16 or Applet.ARG_TYPE_INT

    const val INT_PERCENT = 12 shl 16 or Applet.ARG_TYPE_INT

    const val TEXT_PACKAGE_NAME = 1 shl 16 or Applet.ARG_TYPE_TEXT

    const val TEXT_ACTIVITY = 2 shl 16 or Applet.ARG_TYPE_TEXT

    const val TEXT_PANE_TITLE = 3 shl 16 or Applet.ARG_TYPE_TEXT

    const val TEXT_GESTURES = 4 shl 16 or Applet.ARG_TYPE_TEXT

    const val TEXT_FILE_PATH = 5 shl 16 or Applet.ARG_TYPE_TEXT

    const val TEXT_FORMAT =
        6 shl 16 or Applet.ARG_TYPE_TEXT or MASK_IGNORE_VARIANT_TYPE_WHEN_MATCHING

    const val TEXT_VIBRATION_PATTERN = 7 shl 16 or Applet.ARG_TYPE_TEXT

    fun shouldIgnoreVariantTypeWhenMatching(type: Int): Boolean {
        return type == NONE || type and MASK_IGNORE_VARIANT_TYPE_WHEN_MATCHING != 0
    }

    fun getRawType(variantType: Int): Int {
        return variantType and 0xFFFF
    }
}