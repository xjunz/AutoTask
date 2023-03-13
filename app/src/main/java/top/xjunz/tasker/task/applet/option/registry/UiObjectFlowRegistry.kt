/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.R
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.ContainsUiObject
import top.xjunz.tasker.task.applet.flow.UiObjectMatches
import top.xjunz.tasker.task.applet.value.VariantType

/**
 * @author xjunz 2023/03/13
 */
class UiObjectFlowRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0)
    val containsUiObject = appletOption(R.string.format_contains_ui_object) {
        ContainsUiObject()
    }.withRefArgument<AccessibilityNodeInfo>(R.string.target, R.string.empty)
        .withResult<AccessibilityNodeInfo>(R.string.ui_object)
        .withResult<String>(R.string.ui_object_text)
        .withResult<Int>(R.string.ui_object_center_coordinate, VariantType.INT_COORDINATE)
        .withScopeRegistryId(BootstrapOptionRegistry.ID_UI_OBJECT_CRITERION_REGISTRY)
        .hasCompositeTitle()

    @AppletOrdinal(1)
    val uiObjectMatches = appletOption(R.string.format_ui_object_matches) {
        UiObjectMatches()
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object)
        .withResult<AccessibilityNodeInfo>(R.string.matched_ui_object)
        .withResult<String>(R.string.ui_object_text)
        .withResult<Int>(R.string.ui_object_center_coordinate, VariantType.INT_COORDINATE)
        .withScopeRegistryId(BootstrapOptionRegistry.ID_UI_OBJECT_CRITERION_REGISTRY)
        .hasCompositeTitle()
}