package top.xjunz.tasker.ktx

import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.*
import android.view.View
import androidx.annotation.StringRes
import androidx.core.text.set
import top.xjunz.tasker.ui.ColorSchemes

/**
 * @author xjunz 2022/11/25
 */

private inline fun CharSequence.ensureSpannable(block: (Spannable) -> Unit): CharSequence {
    return (if (this is Spannable) this else SpannableStringBuilder(this)).also(block)
}

fun @receiver:StringRes Int.formatSpans(vararg args: Any): CharSequence {
    val raw = this.str
    val split = raw.split("%s").flatMap {
        it.split("%d")
    }
    var title: CharSequence = split[0]
    for (i in 1..split.lastIndex) {
        val s = split[i]
        val rep: CharSequence = when (val arg = args[i - 1]) {
            is CharSequence -> arg
            else -> arg.toString()
        }
        title += rep + s
    }
    return title
}

private fun CharSequence.setSpan(span: Any) = ensureSpannable {
    it[0..length] = span
}

fun CharSequence.foreColored(color: Int = ColorSchemes.colorPrimary) =
    setSpan(ForegroundColorSpan(color))

fun CharSequence.linked(url: String) = setSpan(URLSpan(url))

fun CharSequence.clickable(underlined: Boolean = false, doOnClick: (View) -> Unit) =
    setSpan(object : ClickableSpan() {
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = underlined
        }

        override fun onClick(widget: View) {
            doOnClick(widget)
        }
    })

fun CharSequence.underlined() = setSpan(UnderlineSpan())

fun CharSequence.bold() = setSpan(StyleSpan(Typeface.BOLD))

fun CharSequence.italic() = setSpan(StyleSpan(Typeface.ITALIC))

fun CharSequence.quoted(strokeColor: Int = ColorSchemes.colorPrimaryContainer) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        setSpan(QuoteSpan(strokeColor, 4, 8))
    } else {
        setSpan(QuoteSpan(strokeColor))
    }

fun CharSequence.backColored() = setSpan(BackgroundColorSpan(ColorSchemes.colorPrimaryContainer))

fun CharSequence.bulleted() = setSpan(BulletSpan(8, ColorSchemes.colorPrimary))

fun CharSequence.relativeSize(size: Float) = setSpan(RelativeSizeSpan(size))

operator fun CharSequence.plus(next: CharSequence): CharSequence {
    if (this is SpannableStringBuilder) {
        return append(next)
    }
    return SpannableStringBuilder(this).append(next)
}
