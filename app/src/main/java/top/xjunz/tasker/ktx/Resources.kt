/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.annotation.*
import top.xjunz.tasker.app
import top.xjunz.tasker.appTheme
import java.io.InputStream

inline val @receiver:StringRes Int.text get() = app.getText(this)

inline val @receiver:StringRes Int.str get() = app.getString(this)

inline val @receiver:ArrayRes Int.array: Array<CharSequence>
    get() = app.resources.getTextArray(this)

@get:ColorInt
inline val @receiver:ColorRes Int.color
    get() = app.getColor(this)

@get:ColorInt
inline val @receiver:AttrRes Int.attrColor
    get() = resolvedId.color

@get:AnyRes
inline val @receiver:AttrRes Int.resolvedId: Int
    get() {
        val value = TypedValue()
        appTheme.resolveAttribute(this, value, true)
        return value.resourceId
    }

inline val @receiver:AttrRes Int.attrColorStateList: ColorStateList
    get() = app.getColorStateList(resolvedId)

fun @receiver:ColorInt Int.toColorStateList(): ColorStateList {
    return ColorStateList.valueOf(this)
}

fun @receiver:StringRes Int.format(vararg any: Any?) = str.format(*any)

inline val Number.dp get() = (app.resources.displayMetrics.density * toFloat()).pxSize

inline val Number.dpFloat get() = app.resources.displayMetrics.density * toFloat()

inline val Float.pxSize get() = (if (this >= 0) this + .5f else this - .5f).toInt()

fun @receiver:RawRes Int.readAsText(): String {
    app.resources.openRawResource(this).use {
        return it.bufferedReader().readText()
    }
}

fun @receiver:RawRes Int.inputStream(): InputStream {
    return app.resources.openRawResource(this)
}