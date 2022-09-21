/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
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


fun View.beginAutoTransition(transition: Transition = AutoTransition()) {
    this as ViewGroup
    TransitionManager.beginDelayedTransition(
        this, transition.setInterpolator(Motions.EASING_EMPHASIZED)
    )
}

