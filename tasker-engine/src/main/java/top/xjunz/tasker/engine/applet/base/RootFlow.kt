package top.xjunz.tasker.engine.applet.base

/**
 * @author xjunz 2022/12/05
 */
class RootFlow : Flow() {

    override fun staticCheckMySelf() {
        super.staticCheckMySelf()
        check(isEnabled)
    }
}