/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.event

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import org.junit.Test
import top.xjunz.tasker.UiAutomationRegistry
import kotlin.coroutines.CoroutineContext

/**
 * @author xjunz 2022/11/01
 */
internal class A11yEventDispatcherTest : CoroutineScope {


    @Test
    fun processAccessibilityEvent() {
        UiAutomationRegistry.getUiAutomation().setOnAccessibilityEventListener {
            println(it)
        }
    }

    override val coroutineContext: CoroutineContext by lazy {
        Handler(Looper.getMainLooper()).asCoroutineDispatcher()
    }
}