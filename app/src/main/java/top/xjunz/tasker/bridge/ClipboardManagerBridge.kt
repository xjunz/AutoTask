/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.content.ClipData
import android.content.ClipboardManager

/**
 * @author xjunz 2022/11/16
 */
object ClipboardManagerBridge {

    fun copyToClipboard(text: CharSequence) {
        ContextBridge.getContext().getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(null, text))
    }
}