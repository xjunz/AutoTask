/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.annotation.StyleableRes
import androidx.core.graphics.Insets
import androidx.core.graphics.applyCanvas
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import top.xjunz.tasker.R
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener
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

fun EditText.setSelectionToEnd() = setSelection(text.length)

fun EditText.setMaxLength(len: Int) {
    filters += InputFilter.LengthFilter(len)
}

inline fun EditText.setOnEnterListener(crossinline block: () -> Unit) {
    setOnKeyListener { _, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
            block()
            return@setOnKeyListener true
        }
        return@setOnKeyListener false
    }
}

fun EditText.setDigits(digits: String) {
    filters += DigitsKeyListener.getInstance(digits)
}

fun EditText.configInputType(type: Class<*>, allowMultiLine: Boolean = false) {
    when (type) {
        String::class.java -> {
            inputType = InputType.TYPE_CLASS_TEXT
            if (allowMultiLine) {
                inputType = inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
        }
        Int::class.java, Long::class.java, Int::class.javaObjectType, Long::class.javaObjectType ->
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        Float::class.java, Float::class.javaObjectType ->
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
    }
}

fun View.beginAutoTransition(target: View, transition: Transition = AutoTransition()) {
    this as ViewGroup
    TransitionManager.beginDelayedTransition(
        this, transition.setInterpolator(Motions.EASING_EMPHASIZED).addTarget(target)
    )
}

fun TextView.setDrawableStart(@DrawableRes res: Int) {
    if (res == View.NO_ID) {
        setDrawableStart(null)
    } else {
        setDrawableStart(res.getDrawable())
    }
}

fun TextView.setDrawableStart(drawable: Drawable?) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
}

fun TextView.setDrawableEnd(@DrawableRes res: Int) {
    if (res == View.NO_ID) {
        setDrawableEnd(null)
    } else {
        setDrawableEnd(res.getDrawable())
    }
}

fun TextView.setDrawableEnd(drawable: Drawable?) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
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
    threshold = Int.MAX_VALUE
    if (setFirstAsText) {
        setText(array[0].toString())
    }
}

fun View.shake() {
    ObjectAnimator.ofFloat(
        this, View.TRANSLATION_X,
        0F, 20F, -20F, 15F, -15F, 10F, -10F, 5F, -5F, 0F
    ).start()
}

/**
 * Set the listener to be notified when the [CompoundButton] changes its checked state because of
 * user interaction (via touch or keyboard).
 */
@SuppressLint("ClickableViewAccessibility")
fun CompoundButton.setOnInteractiveCheckedChangedListener(listener: (v: CompoundButton, isChecked: Boolean) -> Unit) {
    var fromInteraction = false
    setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            fromInteraction = true
        }
        return@setOnTouchListener false
    }
    setOnKeyListener { _, keyCode, event ->
        if (event.action == MotionEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
            fromInteraction = true
        }
        return@setOnKeyListener false
    }
    setOnCheckedChangeListener { v, isChecked ->
        if (fromInteraction) {
            listener.invoke(v, isChecked)
        }
        fromInteraction = false
    }
}

inline fun View.useStyledAttributes(
    set: AttributeSet?, @StyleableRes attrs: IntArray, block: (TypedArray) -> Unit
) {
    val ta = context.theme.obtainStyledAttributes(set, attrs, 0, 0)
    block(ta)
    ta.recycle()
}


@SuppressLint("ClickableViewAccessibility")
fun View.blockTouch() {
    setOnTouchListener { _, _ -> true }
}

fun View.unblockTouch() {
    setOnTouchListener(null)
}

fun View.setHelp(help: CharSequence) {
    background = android.R.attr.selectableItemBackground.resolvedId.getDrawable()
    setAntiMoneyClickListener {
        context.makeSimplePromptDialog(R.string.help.str, help).show()
    }
}