/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.factory

import top.xjunz.tasker.engine.applet.base.Applet

/**
 * @author xjunz 2022/10/28
 */
interface AppletFactory {

    fun createAppletById(id: Int, compatMode: Boolean): Applet
}