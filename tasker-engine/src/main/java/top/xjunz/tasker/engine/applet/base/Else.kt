package top.xjunz.tasker.engine.applet.base

import top.xjunz.shared.utils.unsupportedOperation

/**
 * @author xjunz 2022/11/03
 */
class Else : Do() {

    // Once the previous result is success, do not execute this flow
    override var isAnd = false
        set(value) {
            if (value) unsupportedOperation("Else flow must not have its [isAnd] field true!")
            field = value
        }

    override fun staticCheckMySelf() {
        check(index >= 0 || requireParent()[index - 1] is Do) {
            "Else flow must follow a Then flow!"
        }
    }
}