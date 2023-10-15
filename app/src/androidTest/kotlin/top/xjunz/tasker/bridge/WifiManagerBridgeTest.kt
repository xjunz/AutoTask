/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import top.xjunz.shared.trace.logcat

/**
 * @author xjunz 2023/10/12
 */
class WifiManagerBridgeTest {

    @Test
    fun testNetworkCallback() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        logcat(WifiManagerBridge.wm.connectionInfo)
    }
}