/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.ViewPropertyAnimator

/**
 * @author xjunz 2022/10/13
 */

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