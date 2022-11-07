/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.text.InputFilter
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.core.graphics.Insets
import androidx.core.graphics.applyCanvas
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import top.xjunz.tasker.util.Motions

/**
 * @author xjunz 2022/2/10
 */
inline fun <T : View> T.applySystemInsets(
    type: Int = WindowInsetsCompat.Type.systemBars(),
    crossinline block: (v: T, insets: Insets) -> Unit
) {
    setOnApplyWindowInsetsListener { _, windowInsets ->
        val sysInsets = WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
        block(this, sysInsets.getInsets(type))
        return@setOnApplyWindowInsetsListener windowInsets
    }
}

fun View.drawToBitmapUnsafe(config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
    return Bitmap.createBitmap(width, height, config).applyCanvas {
        translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(this)
    }
}

inline fun View.doOnDetachNotSticky(crossinline action: (view: View) -> Unit) {
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {}

        override fun onViewDetachedFromWindow(view: View) {
            removeOnAttachStateChangeListener(this)
            action(view)
        }
    })
}

inline fun ViewPager2.doOnItemSelected(crossinline block: (Int) -> Unit) {
    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            block(position)
        }
    })
}

inline fun <T : View> T.oneShotApplySystemInsets(
    type: Int = WindowInsetsCompat.Type.systemBars(),
    crossinline block: (v: T, insets: Insets) -> Unit
) {
    setOnApplyWindowInsetsListener { _, windowInsets ->
        val sysInsets = WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
        block(this, sysInsets.getInsets(type))
        setOnApplyWindowInsetsListener(null)
        return@setOnApplyWindowInsetsListener windowInsets
    }
}

inline val EditText.textString get() = text.toString()

fun EditText.setMaxLength(len: Int) {
    filters += InputFilter.LengthFilter(len)
}

fun View.beginAutoTransition(target: View, transition: Transition = AutoTransition()) {
    this as ViewGroup
    TransitionManager.beginDelayedTransition(
        this, transition.setInterpolator(Motions.EASING_EMPHASIZED).addTarget(target)
    )
}

fun View.beginMyselfAutoTransition(transition: Transition = AutoTransition()) {
    rootView.beginAutoTransition(this, transition)
}

fun View.beginAutoTransition(transition: Transition = AutoTransition()) {
    this as ViewGroup
    TransitionManager.beginDelayedTransition(
        this, transition.setInterpolator(Motions.EASING_EMPHASIZED)
    )
}

fun AutoCompleteTextView.setEntries(
    arrayRes: Int,
    setFirstAsText: Boolean = false,
    onItemClicked: ((position: Int) -> Unit)? = null
) {
    val array = arrayRes.array
    setAdapter(
        ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, array)
    )
    if (onItemClicked != null) {
        setOnItemClickListener { _, _, position, _ ->
            onItemClicked(position)
        }
    }
    if (setFirstAsText) {
        threshold = Int.MAX_VALUE
        setText(array[0].toString())
    }
}

fun View.shake() {
    ObjectAnimator.ofFloat(
        this, View.TRANSLATION_X,
        0F, 20F, -20F, 15F, -15F, 10F, -10F, 5F, -5F, 0F
    ).start()
}
