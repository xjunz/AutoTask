/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.text.parseAsHtml
import androidx.databinding.BindingAdapter

/**
 * @author xjunz 2022/04/21
 */
@BindingAdapter("html")
fun TextView.setHtml(html: String) {
    text = html.parseAsHtml()
}

@BindingAdapter("android:contentDescription")
fun View.setContentDescriptionAndTooltip(text: CharSequence) {
    contentDescription = text
    TooltipCompat.setTooltipText(this, contentDescription)
}

@BindingAdapter("linkable")
fun TextView.setLinkable(linkable: Boolean) {
    if (linkable)
        movementMethod = LinkMovementMethod.getInstance()
}
