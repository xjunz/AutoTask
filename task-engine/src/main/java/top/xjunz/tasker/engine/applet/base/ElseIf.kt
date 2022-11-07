package top.xjunz.tasker.engine.applet.base

import top.xjunz.shared.utils.unsupportedOperation

/**
 * @author xjunz 2022/11/03
 */
class ElseIf : If() {

    override var isAnd: Boolean = false
        set(value) {
            if (value) unsupportedOperation("ElseIf flow must not have its [isAnd] field true!")
            field = value
        }

    override fun staticCheckMySelf() {
        super.staticCheckMySelf()
        // Require its previous peer to be a `Then` flow.
        check(index >= 0 || requireParent()[index - 1] is Do) {
            "ElseIf flow must follow a Then flow!"
        }
    }
}