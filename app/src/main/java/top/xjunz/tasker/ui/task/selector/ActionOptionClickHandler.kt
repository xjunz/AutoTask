/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector

import androidx.fragment.app.FragmentManager
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.configInputType
import top.xjunz.tasker.ktx.setMaxLength
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.task.selector.option.ArgumentsEditorDialog
import top.xjunz.tasker.ui.task.selector.option.TimeIntervalEditorDialog

/**
 * @author xjunz 2022/11/22
 */
class ActionOptionClickHandler(private val fragmentManager: FragmentManager) {

    private val factory = AppletOptionFactory

    fun onClick(applet: Applet, option: AppletOption, onCompleted: () -> Unit) {
        applet as Action<*>
        when {
            option.arguments.isEmpty() && applet.valueType == Applet.VAL_TYPE_IRRELEVANT -> onCompleted()

            option == factory.controlActionRegistry.suspension -> {
                TimeIntervalEditorDialog().init(option.currentTitle, applet.value?.casted() ?: 0) {
                    applet.value = it
                    onCompleted()
                }.show(fragmentManager)
            }

            option.arguments.isEmpty() -> {
                if (applet.valueType == Applet.VAL_TYPE_INT) {
                    TextEditorDialog().configEditText {
                        it.setMaxLength(16)
                        it.configInputType(Int::class.java)
                    }.setCaption(option.helpText)
                        .init(option.loadTitle(applet), applet.value?.toString()) {
                            val v = it.toIntOrNull() ?: return@init R.string.error_mal_format.str
                            applet.value = v
                            onCompleted()
                            return@init null
                        }.show(fragmentManager)
                }
            }

            else -> ArgumentsEditorDialog().setAppletOption(applet, option)
                .doOnCompletion(onCompleted).show(fragmentManager)
        }
    }
}