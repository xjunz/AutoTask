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

}