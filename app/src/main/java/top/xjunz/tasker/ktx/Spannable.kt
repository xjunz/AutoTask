package top.xjunz.tasker.ktx

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.*
import android.view.View
import androidx.core.text.set

/**
 * @author xjunz 2022/11/25
 */

private inline fun CharSequence.ensureSpannable(block: (Spannable) -> Unit): CharSequence {
    return (if (this is Spannable) this else SpannableStringBuilder(this)).also(block)
}

private fun CharSequence.setSpan(span: Any) = ensureSpannable {
    it[0..length] = span
}

fun CharSequence.foreColored(color: Int) = setSpan(ForegroundColorSpan(color))

fun CharSequence.linked(url: String) = setSpan(URLSpan(url))

fun CharSequence.clickable(underlined: Boolean = false, doOnClick: (View) -> Unit) =
    setSpan(object : ClickableSpan() {
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = underlined
        }

        override fun onClick(widget: View) {
            doOnClick(widget)
            widget.postInvalidate()
        }
    })

fun CharSequence.underlined() = setSpan(UnderlineSpan())

fun CharSequence.bold() = setSpan(StyleSpan(Typeface.BOLD))

fun CharSequence.italic() = setSpan(StyleSpan(Typeface.ITALIC))

operator fun CharSequence.plus(next: CharSequence): CharSequence {
    if (this is SpannableStringBuilder) {
        return append(next)
    }
    return SpannableStringBuilder(this).append(next)
}
