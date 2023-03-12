/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.event

import android.content.ClipData
import android.content.ClipboardManager
import top.xjunz.tasker.bridge.ContextBridge
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.task.EventDispatcher

/**
 * @author xjunz 2023/03/12
 */
class ClipboardEventDispatcher : EventDispatcher(), ClipboardManager.OnPrimaryClipChangedListener {

    companion object {
        const val EXTRA_PRIMARY_CLIP_TEXT = 0
    }

    private val clipboardManager by lazy {
        ContextBridge.getContext().getSystemService(ClipboardManager::class.java)
    }

    private var previousText: String? = null

    init {
        clipboardManager.addPrimaryClipChangedListener(this)
    }

    override fun destroy() {
        clipboardManager.removePrimaryClipChangedListener(this)
    }

    private fun ClipData.getOrNull(index: Int): ClipData.Item? {
        if (itemCount - 1 >= index) {
            return getItemAt(index)
        }
        return null
    }

    override fun onPrimaryClipChanged() {
        val currentText = clipboardManager.primaryClip?.getOrNull(0)
            ?.coerceToText(ContextBridge.getContext())?.toString()
        if (previousText != currentText && currentText != null) {
            dispatchEvents(Event.obtain(Event.EVENT_ON_PRIMARY_CLIP_CHANGED).apply {
                putExtra(EXTRA_PRIMARY_CLIP_TEXT, currentText)
            })
            previousText = currentText
        }
    }

}