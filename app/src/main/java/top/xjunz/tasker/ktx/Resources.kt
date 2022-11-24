/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.parseAsHtml
import top.xjunz.tasker.app
import java.io.InputStream

inline val @receiver:StringRes Int.text get() = app.getText(this)

inline val @receiver:StringRes Int.str get() = app.getString(this)

inline val @receiver:ArrayRes Int.array: Array<CharSequence>
    get() = app.resources.getTextArray(this)

fun @receiver:DrawableRes Int.getDrawable(): Drawable {
    return ResourcesCompat.getDrawable(app.appTheme.resources, this, app.appTheme)!!
}

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
        app.appTheme.resolveAttribute(this, value, true)
        return value.resourceId
    }

inline val @receiver:AttrRes Int.attrColorStateList: ColorStateList
    get() = app.getColorStateList(resolvedId)

inline val @receiver:AttrRes Int.colorStateList: ColorStateList
    get() = app.appTheme.resources.getColorStateList(this, app.appTheme)

fun @receiver:ColorInt Int.toColorStateList(): ColorStateList {
    return ColorStateList.valueOf(this)
}

fun @receiver:StringRes Int.format(vararg any: Any?) = str.format(*any)

fun @receiver:StringRes Int.formatAsHtml(vararg any: Any?) = str.format(*any).parseAsHtml()

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