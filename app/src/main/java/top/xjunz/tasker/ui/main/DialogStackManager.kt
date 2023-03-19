/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import java.lang.ref.WeakReference
import java.util.*

/**
 * Helper class for simple dialog stack management.
 *
 * @author xjunz 2023/02/21
 */
object DialogStackManager {

    private var callback: WeakReference<Callback>? = null

    private var stack: Stack<StackEntry>? = null

    fun setCallback(callback: Callback) {
        this.callback = WeakReference(callback)
    }

    private fun requireStack(): Stack<StackEntry> {
        if (stack == null) {
            stack = Stack();
        }
        return stack!!
    }

    fun push(tag: String, isFullScreen: Boolean): StackEntry {
        return if (requireStack().isEmpty()) {
            requireStack().push(StackEntry(tag, isFullScreen, null))
        } else {
            requireStack().push(StackEntry(tag, isFullScreen, requireStack().peek()))
        }.also {
            callback?.get()?.onDialogPush(requireStack())
        }
    }

    fun pop(): StackEntry? {
        return if (requireStack().isEmpty()) {
            null
        } else {
            requireStack().pop().also {
                callback?.get()?.onDialogPop(requireStack())
            }
        }
    }

    fun destroyAll() {
        requireStack().clear()
        stack = null
        callback = null
    }

    /** Whether the target is visible. */
    fun isVisible(target: String?): Boolean {
        var cur = if (requireStack().isEmpty()) null else requireStack().peek()
        var occlusion: String? = null
        while (cur != null) {
            if (occlusion == null && cur.isFullScreen) {
                occlusion = cur.tag
            }
            if (cur.tag == target) {
                return occlusion == null || occlusion == target
            }
            cur = cur.previous
        }
        return occlusion == null
    }

    interface Callback {

        fun onDialogPush(stack: Stack<StackEntry>)

        fun onDialogPop(stack: Stack<StackEntry>)
    }

    class StackEntry(val tag: String, val isFullScreen: Boolean, val previous: StackEntry? = null)
}