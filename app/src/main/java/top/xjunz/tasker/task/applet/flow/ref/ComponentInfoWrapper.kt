/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow.ref

import android.content.ComponentName
import android.content.pm.PackageInfo
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.runtime.ComponentInfo
import top.xjunz.tasker.engine.runtime.Referent

/**
 * @author xjunz 2022/10/18
 */
data class ComponentInfoWrapper(
    var packageName: String,
    var activityName: String? = null,
    var paneTitle: String? = null,
) : Referent {

    companion object {

        fun wrap(source: ComponentInfo) =
            ComponentInfoWrapper(source.packageName, source.activityName, source.paneTitle)
    }

    val packageInfo: PackageInfo by lazy {
        PackageManagerBridge.loadPackageInfo(packageName)!!
    }

    val label: String? by lazy {
        PackageManagerBridge.loadLabelOfPackage(packageName).toString()
    }

    override fun getReferredValue(which: Int): Any? {
        return when (which) {
            1 -> packageName
            2 -> label
            else -> super.getReferredValue(which)
        }
    }

    override fun toString(): String {
        return "ComponentInfo(paneTitle=$paneTitle, pkgName=$packageName, actName=$activityName)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComponentInfoWrapper

        if (paneTitle != other.paneTitle) return false
        if (packageName != other.packageName) return false
        if (activityName != other.activityName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = paneTitle?.hashCode() ?: 0
        result = 31 * result + packageName.hashCode()
        result = 31 * result + (activityName?.hashCode() ?: 0)
        return result
    }

    fun getComponentName(): ComponentName? {
        return activityName?.let {
            ComponentName(packageName, it)
        }
    }


}