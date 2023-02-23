/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

/**
 * @author xjunz 2022/11/04
 */
abstract class ControlFlow : Flow() {

    final override var isInvertible: Boolean = false

}