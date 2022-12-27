/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.ViewPropertyAnimator
import androidx.core.animation.doOnEnd

/**
 * @author xjunz 2022/10/13
 */
inline fun Animator.doWhenEnd(crossinline block: (Animator) -> Unit): Animator {
    doOnEnd(block)
    return this
}

inline fun ViewPropertyAnimator.doWhenEnd(crossinline block: () -> Unit): ViewPropertyAnimator {
    setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            block()
            setListener(null)
        }
    })
    return this
}