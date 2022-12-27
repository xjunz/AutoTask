/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * @author xjunz 2022/10/15
 */

fun Bitmap.clip(bounds: Rect): Bitmap {
    return Bitmap.createBitmap(this, bounds.left, bounds.top, bounds.width(), bounds.height())
}