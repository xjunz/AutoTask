/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import org.junit.Test
import top.xjunz.tasker.BuildConfig


/**
 * @author xjunz 2023/01/06
 */
internal class PackageManagerBridgeTest {

    @Test
    fun loadPackageInfo() {
        val info = PackageManagerBridge.loadPackageInfo(BuildConfig.APPLICATION_ID)
        assert(info!!.packageName == BuildConfig.APPLICATION_ID)
    }

    @Test
    fun loadAllPackages() {
    }
}