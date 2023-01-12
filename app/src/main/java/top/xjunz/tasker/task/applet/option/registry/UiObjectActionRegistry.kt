/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.graphics.Point
import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.singleArgAction
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.UiObjectContext
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.Swipe

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
        singleArgAction<UiObjectContext, V> { ctx, value ->
            val node = ctx?.source
            requireNotNull(node) {
                "Node is not captured!"
            }
            if (node.refresh()) block(node, value) else false
        }
    }

    @AppletOrdinal(0x0001)
    val click = simpleUiObjectActionOption(R.string.format_perform_click) {
        if (it.isClickable) {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            uiDevice.wrapUiObject2(it).click()
            true
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object).hasCompositeTitle()

    @AppletOrdinal(0x0002)
    val longClick = simpleUiObjectActionOption(R.string.format_perform_long_click) {
        if (it.isLongClickable) {
            it.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        } else {
            uiDevice.wrapUiObject2(it).longClick()
            true
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object).hasCompositeTitle()

    @AppletOrdinal(0x00_03)
    val drag = uiObjectActionOption<Int>(R.string.format_drag) { node, v ->
        check(v != null)
        uiDevice.wrapUiObject2(node).drag(IntValueUtil.parseCoordinate(v))
        true
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object)
        .withValueArgument<Point>(R.string.specified_coordinate)
        .hasCompositeTitle()

    @AppletOrdinal(0x00_04)
    val swipe = uiObjectActionOption<Long>(R.string.format_swipe_ui_object) { node, v ->
        check(v != null)
        val swipe = Swipe.parse(v)
        uiDevice.wrapUiObject2(node).swipe(swipe.direction, swipe.percent, swipe.speed)
        true
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object).hasCompositeTitle()

    @AppletOrdinal(0x0010)
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