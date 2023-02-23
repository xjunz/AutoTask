/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import java.util.*

/**
 * Helper class for simple dialog stack management.
 *
 * @author xjunz 2023/02/21
 */
object DialogStackManager {

    private var stack: Stack<StackEntry>? = null

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
        }
    }

    fun pop(): StackEntry? {
        return if (requireStack().isEmpty()) null else requireStack().pop()
    }

    fun destroyAll() {
        requireStack().clear()
        stack = null
    }

    /**
     * Whether the target is visible.
     */
    fun isVisible(target: String?): Boolean {
        requireNotNull(target) {
            "Tag is null"
        }
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
        // Not even present in the stack
        return false
    }

    class StackEntry(val tag: String, val isFullScreen: Boolean, val previous: StackEntry? = null)
}