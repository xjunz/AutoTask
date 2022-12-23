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

    override fun staticCheckMyself(): Int {
        if (requireParent().getOrNull(index - 1) !is Do) {
            return StaticError.ERR_ELSEIF_NOT_FOLLOWING_DO
        }
        return super.staticCheckMyself()
    }
}