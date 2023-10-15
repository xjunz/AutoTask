/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.tasker.engine.applet.base.Applet

/**
 * @author xjunz 2023/09/24
 */
abstract class Action : Applet() {

    final override val supportsAnywayRelation: Boolean = true

}
