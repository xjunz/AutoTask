/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.singleArgAction
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletOption

/**
 * @author xjunz 2022/11/15
 */
class UiObjectActionRegistry(id: Int) : AppletOptionRegistry(id) {

    private inline fun simpleUiObjectActionOption(
        title: Int, crossinline block: (AccessibilityNodeInfo) -> Boolean
    ): AppletOption {
        return uiObjectActionOption<Unit>(title) { node, _ ->
            block(node)
        }
    }

    private inline fun <reified V> uiObjectActionOption(
        title: Int, crossinline block: (AccessibilityNodeInfo, V?) -> Boolean
    ) = appletOption(title) {
        singleArgAction<AccessibilityNodeInfo, V> { node, value ->
            requireNotNull(node) {
                "Node is not captured!"
            }
            if (node.refresh()) block(node, value) else false
        }
    }

    @AppletCategory(0x0001)
    val click = simpleUiObjectActionOption(R.string.format_perform_click) {
        if (it.isClickable) {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            uiDevice.wrapUiObject2(it).click()
            true
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object).hasCompositeTitle()

    @AppletCategory(0x0002)
    val longClick = simpleUiObjectActionOption(R.string.format_perform_long_click) {
        if (it.isLongClickable) {
            it.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        } else {
            uiDevice.wrapUiObject2(it).longClick()
            true
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object).hasCompositeTitle()

    @AppletCategory(0x0010)
    val setText = uiObjectActionOption<String>(R.string.format_perform_input_text) { node, value ->
        if (!node.isEditable) false
        else {
            uiDevice.wrapUiObject2(node).text = value
            true
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.input_field)
        .withArgument<String>(R.string.text)
        .hasCompositeTitle()

}