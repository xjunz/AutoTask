/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector

import androidx.fragment.app.FragmentManager
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.criterion.Criterion
import top.xjunz.tasker.engine.applet.util.isAttached
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.task.selector.argument.ArgumentsEditorDialog

/**
 * @author xjunz 2022/10/08
 */
open class AppletOptionClickHandler(private val fragmentManager: FragmentManager) {

    private val factory = AppletOptionFactory

    fun onClick(applet: Applet, onCompleted: () -> Unit) {
        onClick(applet, factory.requireOption(applet), onCompleted)
    }

    open fun onClick(applet: Applet, option: AppletOption, onCompleted: () -> Unit) {
        when {
            option.arguments.isNotEmpty() -> ArgumentsEditorDialog()
                .setAppletOption(applet, option)
                .doOnCompletion(onCompleted).show(fragmentManager)

            applet is Criterion<*, *> -> {
                if (applet.isAttached && applet.isInvertible) applet.toggleInversion()
                onCompleted()
            }

            else -> onCompleted()
        }
    }
}