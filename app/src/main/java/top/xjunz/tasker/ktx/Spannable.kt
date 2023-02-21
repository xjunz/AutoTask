/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.*
import android.view.View
import androidx.annotation.StringRes
import androidx.core.text.set
import top.xjunz.tasker.ui.main.ColorScheme
import top.xjunz.tasker.util.ClickListenerUtil

/**
 * @author xjunz 2022/11/25
 */

private inline fun CharSequence.ensureSpannable(block: (Spannable) -> Unit): CharSequence {
    return (if (this is Spannable) this else SpannableStringBuilder(this)).also(block)
}

fun String.formatSpans(vararg args: Any): CharSequence {
    val split = split("%s").flatMap {
        it.split("%d")
    }
    var title: CharSequence = split[0]
    for (i in 1..split.lastIndex) {
        val s = split[i]
        val rep = when (val arg = args[i - 1]) {
            is CharSequence -> arg
            else -> arg.toString()
        }
        title += rep + s
    }
    return title
}

fun @receiver:StringRes Int.formatSpans(vararg args: Any): CharSequence {
    return str.formatSpans(*args)
}

private fun CharSequence.setSpan(span: Any) = ensureSpannable {
    it[0..length] = span
}

fun CharSequence.foreColored(color: Int = ColorScheme.colorPrimary) =
    setSpan(ForegroundColorSpan(color))

fun CharSequence.linked(url: String) = setSpan(URLSpan(url))

fun CharSequence.clickable(underlined: Boolean = false, doOnClick: (View) -> Unit) =
    setSpan(object : ClickableSpan() {

        var prevClickTimestamp = -1L

        override fun onClick(v: View) {
            val uptime = SystemClock.uptimeMillis()
            if (prevClickTimestamp == -1L
                || uptime - prevClickTimestamp >= ClickListenerUtil.DOUBLE_CLICK_THRESHOLD_INTERVAL
            ) {
                doOnClick(v)
                prevClickTimestamp = uptime
            }
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = underlined
        }
    })

fun CharSequence.underlined() = setSpan(UnderlineSpan())

fun CharSequence.bold() = setSpan(StyleSpan(Typeface.BOLD))

fun CharSequence.italic() = setSpan(StyleSpan(Typeface.ITALIC))

fun CharSequence.quoted(strokeColor: Int = ColorScheme.colorPrimaryContainer) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        setSpan(QuoteSpan(strokeColor, 4, 8))
    } else {
        setSpan(QuoteSpan(strokeColor))
    }

fun CharSequence.strikeThrough() = setSpan(StrikethroughSpan())

fun CharSequence.backColored(color: Int = ColorScheme.colorPrimaryContainer) =
    setSpan(BackgroundColorSpan(color))

fun CharSequence.bulleted() = setSpan(BulletSpan(8, ColorScheme.colorPrimary))

fun CharSequence.relativeSize(size: Float) = setSpan(RelativeSizeSpan(size))

operator fun CharSequence.plus(next: CharSequence?): CharSequence {
    if (this is SpannableStringBuilder) {
        return append(next)
    }
    return SpannableStringBuilder(this).append(next)
}
