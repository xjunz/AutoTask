/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.ClipboardManagerBridge
import top.xjunz.tasker.engine.applet.action.LambdaReferenceAction.Companion.referenceAction
import top.xjunz.tasker.engine.applet.action.Processor.Companion.unaryArgProcessor
import top.xjunz.tasker.engine.applet.action.unaryArgValueAction
import top.xjunz.tasker.ktx.firstGroupValue
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.task.applet.anno.AppletOrdinal

/**
 * @author xjunz 2023/01/06
 */
class TextActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0000)
    val logcatText = appletOption(R.string.logcat_text) {
        referenceAction<String> { args, value, runtime ->
            runtime.snapshot?.logcat(value?.format(*args))
            true
        }
    }.withUnaryArgument<String>(
        name = R.string.log_text,
        isRef = false,
        isCollection = true
    ).withResult<String>(R.string.displayed_text)
        .withDescriber<String> { applet, t ->
            val bolds = applet.references.values.map {
                ("\${$it}").foreColored()
            }.toTypedArray()
            t?.formatSpans(*bolds)
        }.withTitleModifier(R.string.tip_logcat)

    @AppletOrdinal(0x0001)
    val extractText = appletOption(R.string.format_extract_text) {
        unaryArgProcessor<String, String> { arg, v ->
            if (v == null) null else arg?.firstGroupValue(v)
        }
    }.withRefArgument<String>(R.string.text)
        .withValueArgument<String>(R.string.regex)
        .withResult<String>(R.string.extracted_text)
        .withHelperText(R.string.help_extract_text)
        .hasCompositeTitle()

    @AppletOrdinal(0x0002)
    val copyText = appletOption(R.string.format_copy_text) {
        unaryArgValueAction<String> { text ->
            ClipboardManagerBridge.copyToClipboard(text)
            true
        }
    }.withUnaryArgument<String>(R.string.text)
        .hasCompositeTitle()

    @AppletOrdinal(0x0003)
    val makeToast = appletOption(R.string.format_make_toast) {
        referenceAction<String> { args, value, _ ->
            currentService.overlayToastBridge.showOverlayToast(value?.format(*args))
            true
        }
    }.withUnaryArgument<String>(
        name = R.string.msg_to_toast,
        substitution = R.string.text_message,
        isRef = false,
        isCollection = true
    ).withResult<String>(R.string.displayed_text)
        .withDescriber<String> { applet, t ->
            val bolds = applet.references.values.map {
                ("\${$it}").foreColored()
            }.toTypedArray()
            t?.formatSpans(*bolds)
        }.hasCompositeTitle()
}