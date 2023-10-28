/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.Direction
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.createProcessor
import top.xjunz.tasker.engine.applet.action.doubleArgsAction
import top.xjunz.tasker.engine.applet.action.emptyArgOptimisticAction
import top.xjunz.tasker.engine.applet.action.simpleSingleArgAction
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.isPrivilegedProcess
import top.xjunz.tasker.ktx.array
import top.xjunz.tasker.ktx.ensureRefresh
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.ClickButtonWithText
import top.xjunz.tasker.task.applet.flow.ClickUiObjectIfExists
import top.xjunz.tasker.task.applet.flow.ClickUiObjectWithText
import top.xjunz.tasker.task.applet.flow.ForEachUiScrollable
import top.xjunz.tasker.task.applet.flow.InputTextToFirstTextField
import top.xjunz.tasker.task.applet.flow.ScrollIntoUiObject
import top.xjunz.tasker.task.applet.flow.ref.UiObjectReferent
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.ScrollMetrics
import top.xjunz.tasker.task.applet.value.SwipeMetrics
import top.xjunz.tasker.task.applet.value.VariantArgType

/**
 * @author xjunz 2022/11/15
 */
class UiObjectActionRegistry(id: Int) : AppletOptionRegistry(id) {

    override val categoryNames: IntArray
        get() = intArrayOf(R.string.quick_create, R.string.universal)

    private val swipeDirections by lazy {
        R.array.swipe_directions.array
    }

    private inline fun simpleUiObjectActionOption(
        title: Int,
        crossinline block: suspend (AccessibilityNodeInfo) -> Boolean
    ): AppletOption {
        return appletOption(title) {
            simpleSingleArgAction<AccessibilityNodeInfo> {
                requireNotNull(it) {
                    "The node is not captured!"
                }
                it.ensureRefresh()
                block(it)
            }
        }
    }

    private inline fun <reified V> uiObjectActionOption(
        title: Int,
        crossinline block: suspend (AccessibilityNodeInfo, V?) -> Boolean
    ): AppletOption {
        return appletOption(title) {
            doubleArgsAction<AccessibilityNodeInfo, V> { node, value, _ ->
                requireNotNull(node) {
                    "The node is not captured!"
                }
                node.ensureRefresh()
                block(node, value)
            }
        }
    }

    private fun AppletOption.withUiObjectResults(): AppletOption {
        return withResult<AccessibilityNodeInfo>(R.string.matched_ui_object)
            .withResult<String>(R.string.ui_object_text)
            .withResult<Int>(R.string.ui_object_center_coordinate, VariantArgType.INT_COORDINATE)
    }

    @AppletOrdinal(0x0001)
    val clickIfExits = appletOption(R.string.click_if_exists) {
        ClickUiObjectIfExists()
    }.withScopeRegistryId(BootstrapOptionRegistry.ID_UI_OBJECT_CRITERION_REGISTRY)
        .withUiObjectResults()

    @AppletOrdinal(0x0002)
    val clickButtonWithText = appletOption(R.string.format_click_button_with_text) {
        ClickButtonWithText()
    }.withUnaryArgument<String>(R.string.specified_text)
        .withUiObjectResults()
        .hasCompositeTitle()

    @AppletOrdinal(0x0003)
    val clickUiObjectWithText = appletOption(R.string.format_click_uiobject_with_text) {
        ClickUiObjectWithText()
    }.withUnaryArgument<String>(R.string.specified_text)
        .withUiObjectResults()
        .hasCompositeTitle()

    @AppletOrdinal(0x0004)
    val setTextToFirstTextField = appletOption(R.string.format_input_text_to_first_text_field) {
        InputTextToFirstTextField()
    }.withUnaryArgument<String>(R.string.text)
        .hasCompositeTitle()

    @AppletOrdinal(0x0101)
    val click = simpleUiObjectActionOption(R.string.format_perform_click) {
        it.ensureRefresh()
        if (it.isClickable) {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            if (isPrivilegedProcess) {
                uiDevice.wrapUiObject(it).click()
            } else {
                // StrokeDescription must have a positive duration in a11y mode.
                uiDevice.wrapUiObject(it).click(5)
            }
            true
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object).hasCompositeTitle()

    @AppletOrdinal(0x0102)
    val longClick = simpleUiObjectActionOption(R.string.format_perform_long_click) {
        it.ensureRefresh()
        if (it.isLongClickable) {
            it.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        } else {
            uiDevice.wrapUiObject(it).longClick()
            true
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object).hasCompositeTitle()

    @AppletOrdinal(0x0103)
    val drag = uiObjectActionOption<Int>(R.string.format_drag) { node, v ->
        check(v != null)
        node.ensureRefresh()
        uiDevice.wrapUiObject(node).drag(IntValueUtil.parseXY(v))
        true
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object)
        .withUnaryArgument<Int>(
            R.string.specified_coordinate,
            variantValueType = VariantArgType.INT_COORDINATE
        )
        .withSingleValueDescriber<Int> {
            val p = IntValueUtil.parseXY(it)
            R.string.format_coordinate.format(p.x, p.y)
        }
        .hasCompositeTitle()

    @AppletOrdinal(0x0104)
    val swipe = uiObjectActionOption<Long>(R.string.format_swipe_ui_object) { node, v ->
        check(v != null)
        node.ensureRefresh()
        val swipe = SwipeMetrics.parse(v)
        uiDevice.wrapUiObject(node).swipe(swipe.direction, swipe.percent, swipe.speed)
        true
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object)
        .withValueArgument<Long>(R.string.swipe_args, VariantArgType.BITS_SWIPE)
        .withSingleValueDescriber<Long> {
            val swipe = SwipeMetrics.parse(it)
            val direction = swipeDirections[Direction.ALL_DIRECTIONS.indexOf(swipe.direction)]
            R.string.format_swipe_args.format(direction, (swipe.percent * 100).toInt(), swipe.speed)
        }
        .hasCompositeTitle()

    @AppletOrdinal(0x0110)
    val setText = appletOption(R.string.format_perform_input_text) {
        doubleArgsAction<AccessibilityNodeInfo, String> { node, text, _ ->
            requireNotNull(node)
            node.ensureRefresh()
            if (!node.isEditable) {
                false
            } else {
                uiDevice.wrapUiObject(node).setText(text)
                true
            }
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.input_field)
        .withUnaryArgument<String>(R.string.text)
        .hasCompositeTitle()

    private val scrollDescriber = { it: Long ->
        val metrics = ScrollMetrics.parse(it)
        val direction = swipeDirections[Direction.ALL_DIRECTIONS.indexOf(metrics.direction)]
        R.string.format_scroll_args.format(direction, metrics.speed)
    }

    @AppletOrdinal(0x0120)
    val scrollForward = appletOption(R.string.scroll_list) {
        doubleArgsAction<AccessibilityNodeInfo, Long> { node, v, _ ->
            node?.ensureRefresh()
            val metrics = ScrollMetrics.parse(v!!)
            if (metrics.direction == Direction.UP || metrics.direction == Direction.LEFT) {
                uiDevice.wrapUiScrollable(metrics.isVertical, node!!).scrollBackward(metrics.steps)
            } else {
                uiDevice.wrapUiScrollable(metrics.isVertical, node!!).scrollForward(metrics.steps)
            }
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.list)
        .withValueArgument<Long>(R.string.swipe_args, VariantArgType.BITS_SCROLL)
        .withSingleValueDescriber(scrollDescriber)

    @AppletOrdinal(0x0121)
    val scrollToEnd = appletOption(R.string.scroll_to_end) {
        doubleArgsAction<AccessibilityNodeInfo, Long> { node, v, _ ->
            node?.ensureRefresh()
            val metrics = ScrollMetrics.parse(v!!)
            if (metrics.direction == Direction.UP || metrics.direction == Direction.LEFT) {
                uiDevice.wrapUiScrollable(metrics.isVertical, node!!)
                    .scrollToBeginning(metrics.steps)
            } else {
                uiDevice.wrapUiScrollable(metrics.isVertical, node!!).scrollToEnd(metrics.steps)
            }

        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.list)
        .withValueArgument<Long>(R.string.swipe_args, VariantArgType.BITS_SCROLL)
        .withSingleValueDescriber(scrollDescriber)

    @AppletOrdinal(0x0130)
    val scrollIntoUiObject = appletOption(R.string.scroll_into_ui_object) {
        ScrollIntoUiObject()
    }.withScopeRegistryId(BootstrapOptionRegistry.ID_UI_OBJECT_CRITERION_REGISTRY)
        .withRefArgument<AccessibilityNodeInfo>(R.string.list)
        .withValueArgument<Long>(R.string.swipe_args, VariantArgType.BITS_SCROLL)
        .withUiObjectResults()
        .withSingleValueDescriber(scrollDescriber)
        .hasCompositeTitle()

    @AppletOrdinal(0x0140)
    val forEachUiScrollable = appletOption(R.string.for_each_ui_scrollable) {
        ForEachUiScrollable()
    }.withRefArgument<AccessibilityNodeInfo>(R.string.list)
        .withValueArgument<Long>(R.string.swipe_args, VariantArgType.BITS_SCROLL)
        .withResult<AccessibilityNodeInfo>(R.string.current_child)
        .withResult<Int>(R.string.current_child_count)
        .withSingleValueDescriber(scrollDescriber)
        .hasCompositeTitle()

    @AppletOrdinal(0x0148)
    val getChildAt = appletOption(R.string.format_get_child_at) {
        createProcessor { args, _ ->
            val parent = args[0] as AccessibilityNodeInfo
            parent.ensureRefresh()
            val child = parent.getChild(args[1] as Int - 1)
            if (child != null) {
                UiObjectReferent(child).asResult()
            } else {
                AppletResult.EMPTY_FAILURE
            }
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.parent_node, R.string.certain_ui_object)
        .withValueArgument<Int>(R.string.which_one)
        .withSingleValueDescriber<Int> {
            R.string.format_number.formatSpans(it.toString().foreColored())
        }
        .withResult<AccessibilityNodeInfo>(R.string.child_node)
        .withResult<String>(R.string.ui_object_text)
        .hasCompositeTitle()

    @AppletOrdinal(0x0150)
    val drawNodeBounds = appletOption(R.string.format_draw_node_bounds) {
        simpleSingleArgAction<AccessibilityNodeInfo> {
            if (it != null) {
                currentService.overlayToastBridge.drawAccessibilityBounds(it)
                true
            } else {
                false
            }
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object)
        .hasCompositeTitle()

    @AppletOrdinal(0x0151)
    val clearNodeBounds = appletOption(R.string.clear_node_bounds) {
        emptyArgOptimisticAction {
            currentService.overlayToastBridge.clearAccessibilityBounds()
        }
    }
}