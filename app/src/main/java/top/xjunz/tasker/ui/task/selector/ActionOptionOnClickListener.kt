package top.xjunz.tasker.ui.task.selector

import androidx.fragment.app.FragmentManager
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.ktx.configInputType
import top.xjunz.tasker.ktx.setMaxLength
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.task.selector.option.ArgumentsEditorDialog

/**
 * @author xjunz 2022/11/22
 */
class ActionOptionOnClickListener(private val fragmentManager: FragmentManager) {

    fun onClick(applet: Applet, option: AppletOption, onCompleted: () -> Unit) {
        applet as Action
        when {
            option.arguments.isEmpty() && applet.valueType == AppletValues.VAL_TYPE_IRRELEVANT -> onCompleted()

            option.arguments.isEmpty() -> {
                if (applet.valueType == AppletValues.VAL_TYPE_INT) {
                    TextEditorDialog().configEditText {
                        it.setMaxLength(16)
                        it.configInputType(Int::class.java)
                    }.setCaption(option.helpText)
                        .init(option.getTitle(applet)!!, applet.value?.toString()) {
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