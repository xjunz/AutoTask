/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.content.pm.PackageInfo
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.runtime.ComponentInfoWrapper

/**
 * @author xjunz 2022/10/02
 */
class ComponentInfoContext(
    val packageName: String,
    val activityName: String?,
    val panelTitle: String?
) {

    companion object {

        fun from(info: ComponentInfoWrapper): ComponentInfoContext {
            return ComponentInfoContext(info.pkgName, info.actName, info.paneTitle)
        }
    }

    val packageInfo: PackageInfo by lazy {
        PackageManagerBridge.loadPackageInfo(packageName)!!
    }
}