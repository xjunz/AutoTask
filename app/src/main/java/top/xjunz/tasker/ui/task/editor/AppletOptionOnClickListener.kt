package top.xjunz.tasker.ui.task.editor

import androidx.fragment.app.Fragment
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.task.factory.AppletOption
import top.xjunz.tasker.task.factory.FlowFactory
import top.xjunz.tasker.task.factory.PackageAppletFactory
import top.xjunz.tasker.ui.task.editor.selector.ComponentSelectorDialog

/**
 * @author xjunz 2022/10/08
 */
object AppletOptionOnClickListener {

    fun onClick(option: AppletOption, fragment: Fragment) {
        // val applet = option.createApplet()
        if (option.factoryId == FlowFactory.ID_PKG_APPLET_FACTORY) {
            if (option.appletId == PackageAppletFactory.APPLET_PKG_COLLECTION) {
                ComponentSelectorDialog().setTitle(option.currentTitle!!)
                    .show(fragment.parentFragmentManager)
            } else if (option.appletId == PackageAppletFactory.APPLET_ACT_COLLECTION) {
                ComponentSelectorDialog().setTitle(option.currentTitle!!)
                    .setMode(ComponentSelectorDialog.MODE_ACTIVITY)
                    .show(fragment.parentFragmentManager)
            }
        }
    }
}