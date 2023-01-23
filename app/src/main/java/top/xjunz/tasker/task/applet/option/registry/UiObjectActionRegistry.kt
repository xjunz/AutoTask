/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.Direction
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.singleArgAction
import top.xjunz.tasker.ktx.array
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.Swipe
import top.xjunz.tasker.task.applet.value.VariantType

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
                "Node is not captured?!"
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
        .withArgument<Int>(R.string.specified_coordinate, VariantType.INT_COORDINATE)
        .withValueDescriber<Int> {
            val p = IntValueUtil.parseCoordinate(it)
            R.string.format_coordinate.format(p.x, p.y)
        }
        .hasCompositeTitle()

    @AppletOrdinal(0x00_04)
    val swipe = uiObjectActionOption<Long>(R.string.format_swipe_ui_object) { node, v ->
        check(v != null)
        val swipe = Swipe.parse(v)
        uiDevice.wrapUiObject2(node).swipe(swipe.direction, swipe.percent, swipe.speed)
        true
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object)
        .withValueArgument<Long>(R.string.swipe_args, VariantType.BITS_SWIPE)
        .withValueDescriber<Long> {
            val swipe = Swipe.parse(it)
            val direction =
                R.array.swipe_directions.array[Direction.ALL_DIRECTIONS.indexOf(swipe.direction)]
            R.string.format_swipe_args.format(direction, (swipe.percent * 100).toInt(), swipe.speed)
        }
        .hasCompositeTitle()

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