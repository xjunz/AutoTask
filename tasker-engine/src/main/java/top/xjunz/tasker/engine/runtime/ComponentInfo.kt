/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

/**
 * @author xjunz 2023/01/15
 */
class ComponentInfo {

    var packageName: String? = null

    var activityName: String? = null

    var paneTitle: String? = null

    override fun toString(): String {
        return "ComponentInfo(packageName='$packageName', activityName=$activityName, paneTitle=$paneTitle)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentInfo) return false

        if (packageName != other.packageName) return false
        if (activityName != other.activityName) return false
        if (paneTitle != other.paneTitle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName?.hashCode() ?: 0
        result = 31 * result + (activityName?.hashCode() ?: 0)
        result = 31 * result + (paneTitle?.hashCode() ?: 0)
        return result
    }

}