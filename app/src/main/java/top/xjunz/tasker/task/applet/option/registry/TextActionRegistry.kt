/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.ClipboardManagerBridge
import top.xjunz.tasker.engine.applet.action.createProcessor
import top.xjunz.tasker.engine.applet.action.optimisticVarRefAction
import top.xjunz.tasker.engine.applet.action.simpleSingleNonNullArgAction
import top.xjunz.tasker.ktx.firstGroupValue
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.value.VariantArgType

/**
 * @author xjunz 2023/01/06
 */
class TextActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0000)
    val logcatText = appletOption(R.string.logcat_text) {
        optimisticVarRefAction<String> { value, refs, runtime ->
            runtime.snapshot?.logcat(value.format(*refs))
        }
    }.withValueArgument<String>(R.string.logcat_text, VariantArgType.TEXT_FORMAT)
        .withResult<String>(R.string.displayed_text)
        .withSingleValueAppletDescriber<String> { applet, t ->
            val bolds = applet.references.values.map {
                ("\${$it}").foreColored()
            }.toTypedArray()
            t?.formatSpans(*bolds)
        }.withTitleModifier(R.string.tip_logcat)

    @AppletOrdinal(0x0001)
    val extractText = appletOption(R.string.format_extract_text) {
        createProcessor { args, _ ->
            val src = args[0] as? String
            val regex = args[1] as? String
            if (regex == null) null else src?.firstGroupValue(regex)
        }
    }.withRefArgument<String>(R.string.text)
        .withValueArgument<String>(R.string.regex)
        .withResult<String>(R.string.extracted_text)
        .withHelperText(R.string.help_extract_text)
        .hasCompositeTitle()

    @AppletOrdinal(0x0002)
    val copyText = appletOption(R.string.format_copy_text) {
        simpleSingleNonNullArgAction<String> { text ->
            ClipboardManagerBridge.copyToClipboard(text)
            true
        }
    }.withUnaryArgument<String>(R.string.text)
        .hasCompositeTitle()

    @AppletOrdinal(0x0003)
    val makeToast = appletOption(R.string.make_toast) {
        optimisticVarRefAction<String> { value, refs, _ ->
            val text = value.format(*refs)
            currentService.overlayToastBridge.showOverlayToast(text)
        }
    }.withValueArgument<String>(R.string.msg_to_toast, VariantArgType.TEXT_FORMAT)
        .withResult<String>(R.string.displayed_text)
        .withSingleValueAppletDescriber<String> { applet, t ->
            val bolds = applet.references.values.map {
                ("\${$it}").foreColored()
            }.toTypedArray()
            t?.formatSpans(*bolds)
        }
}