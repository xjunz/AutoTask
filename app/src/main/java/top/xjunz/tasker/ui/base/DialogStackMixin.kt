/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import top.xjunz.tasker.ui.main.DialogStackManager
import top.xjunz.tasker.ui.main.MainViewModel
import top.xjunz.tasker.util.Motions

/** @author xjunz 2023/02/21 */
class DialogStackMixin(
    private val dialogFragment: DialogFragment,
    private val isFullScreen: Boolean
) {

    companion object {

        fun animateExit(window: Window, onEnd: () -> Unit = {}) {
            window.peekDecorView().animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .withEndAction(onEnd)
                .setDuration(500)
                .setInterpolator(Motions.EASING_EMPHASIZED)
                .start()
        }

        fun animateReturn(window: Window, onEnd: () -> Unit = {}) {
            // Remove the decor view animation temporarily
            window.setWindowAnimations(0)
            window.peekDecorView().animate()
                .setDuration(200)
                .scaleX(1f)
                .scaleY(1f)
                .withEndAction(onEnd)
                .setInterpolator(Motions.EASING_EMPHASIZED)
                .start()
        }
    }

    private var isStopped = false

    private var isExiting = false

    private val tag get() = dialogFragment.tag

    private val dialog get() = dialogFragment.requireDialog()

    private val window get() = dialog.window!!

    private val windowAnimationStyle = window.attributes.windowAnimations

    private val mainViewModel by dialogFragment.activityViewModels<MainViewModel>()

    fun doOnViewCreated() {
        mainViewModel.notifyDialogShown(dialogFragment.tag, isFullScreen)
        mainViewModel.doOnDialogShown(dialogFragment) {
            if (it.tag != tag && dialog.isShowing && it.isFullScreen) {
                isExiting = true
                isStopped = true
                animateExit()
            }
        }
        mainViewModel.doOnDialogDismissed(dialogFragment) {
            if (it.tag != tag && (isExiting || isStopped)
                && DialogStackManager.isVisible(tag)
            ) {
                isExiting = false
                isStopped = false
                animateReturn()
            }
        }
    }

    fun doOnStart() {
        // Override system behaviour
        if (isStopped) {
            dialogFragment.onStop()
        }
    }

    fun doOnDismissOrCancel() {
        if (dialogFragment.view != null) {
            mainViewModel.notifyDialogDismissed()
        }
    }

    private fun animateExit() {
        animateExit(window) {
            isExiting = false
            dialogFragment.onStop()
        }
    }

    private fun animateReturn() {
        dialogFragment.onStart()
        // Remove the decor view animation temporarily
        animateReturn(window) {
            // The dialog may be dismissed at the moment
            dialogFragment.dialog?.window?.setWindowAnimations(windowAnimationStyle)
        }
    }

}