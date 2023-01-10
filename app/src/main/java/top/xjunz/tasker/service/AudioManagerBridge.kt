/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import android.media.AudioManager
import top.xjunz.tasker.bridge.ContextBridge

/**
 * @author xjunz 2023/01/09
 */
object AudioManagerBridge {

    private val audioManager by lazy {
        ContextBridge.getContext().getSystemService(AudioManager::class.java)
    }

    fun registerAutomatorServiceBreakpoint() {

    }
}