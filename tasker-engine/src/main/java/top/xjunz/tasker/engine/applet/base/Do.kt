package top.xjunz.tasker.engine.applet.base

import top.xjunz.shared.utils.unsupportedOperation

/**
 * @author xjunz 2022/11/03
 */
open class Do : ControlFlow() {

    override var isAnd: Boolean = false
        set(value) {
            if (value) unsupportedOperation("Then flow must not have its [isAnd] field true!")
            field = value
        }
}