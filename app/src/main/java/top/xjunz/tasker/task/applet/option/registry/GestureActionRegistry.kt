/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.singleNonNullArgAction
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.action.GestureAction
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.VariantArgType
import top.xjunz.tasker.task.gesture.SerializableInputEvent

/**
 * @author xjunz 2023/01/07
 */
class GestureActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x00_00)
    val click = appletOption(R.string.format_click) {
        singleNonNullArgAction<Int> {
            val point = IntValueUtil.parseXY(it)
            uiDevice.click(point.x, point.y)
        }
    }.withSingleValueDescriber<Int> {
        val point = IntValueUtil.parseXY(it)
        R.string.format_coordinate.format(point.x, point.y)
    }.withUnaryArgument<Int>(
        R.string.specified_coordinate,
        variantType = VariantArgType.INT_COORDINATE
    ).hasCompositeTitle()

    @AppletOrdinal(0x00_01)
    val longClick = appletOption(R.string.format_long_click) {
        singleNonNullArgAction<Int> {
            val point = IntValueUtil.parseXY(it)
            uiDevice.longClick(point.x, point.y)
        }
    }.withUnaryArgument<Int>(
        R.string.specified_coordinate,
        variantType = VariantArgType.INT_COORDINATE
    ).hasCompositeTitle()

    @AppletOrdinal(0x00_02)
    val performCustomGestures = appletOption(R.string.perform_custom_gestures) {
        GestureAction()
    }.withUnaryArgument<String>(
        R.string.gesture, variantType = VariantArgType.TEXT_GESTURES
    ).withResult<String>(R.string.gesture, VariantArgType.TEXT_GESTURES)
        .withSingleValueDescriber<List<SerializableInputEvent>> {
            R.string.format_gestures_desc.formatSpans(it.size.toString().foreColored())
        }

}