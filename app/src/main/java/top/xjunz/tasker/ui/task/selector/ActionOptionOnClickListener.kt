package top.xjunz.tasker.ui.task.selector

import androidx.fragment.app.Fragment
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.task.selector.option.ArgumentsEditorDialog

/**
 * @author xjunz 2022/11/22
 */
class ActionOptionOnClickListener(
    fragment: Fragment,
    factory: AppletOptionFactory
) : AppletOptionOnClickListener(fragment, factory) {

    override fun onClick(applet: Applet, option: AppletOption, onCompleted: () -> Unit) {
        applet as Action
        when {
            option.arguments.isEmpty() -> onCompleted()
            else -> ArgumentsEditorDialog().setAppletOption(applet,option).show(fragmentManager)
        }
    }
}