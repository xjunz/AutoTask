/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.unaryArgValueAction
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.VariantType

/**
 * @author xjunz 2023/01/07
 */
class GestureActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x00_01)
    val click = appletOption(R.string.format_click) {
        unaryArgValueAction<Int> {
            val point = IntValueUtil.parseCoordinate(it)
            uiDevice.click(point.x, point.y)
        }
    }.withValueDescriber<Int> {
        val point = IntValueUtil.parseCoordinate(it)
        R.string.format_coordinate.format(point.x, point.y)
    }.withUnaryArgument<Int>(
        R.string.specified_coordinate,
        variantType = VariantType.INT_COORDINATE
    ).hasCompositeTitle()

    @AppletOrdinal(0x00_02)
    val longClick = appletOption(R.string.format_long_click) {
        unaryArgValueAction<Int> {
            val point = IntValueUtil.parseCoordinate(it)
            uiDevice.longClick(point.x, point.y)
        }
    }.withUnaryArgument<Int>(
        R.string.specified_coordinate,
        variantType = VariantType.INT_COORDINATE
    ).hasCompositeTitle()
}