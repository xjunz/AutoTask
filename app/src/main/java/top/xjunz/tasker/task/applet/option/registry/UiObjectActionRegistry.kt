/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.Direction
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.*
import top.xjunz.tasker.engine.applet.action.LambdaReferenceAction.Companion.referenceAction
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.isPrivilegedProcess
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.ForEachUiScrollable
import top.xjunz.tasker.task.applet.flow.ScrollIntoUiObject
import top.xjunz.tasker.task.applet.flow.ref.UiObjectReferent
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.ScrollMetrics
import top.xjunz.tasker.task.applet.value.SwipeMetrics
import top.xjunz.tasker.task.applet.value.VariantType

/**
 * @author xjunz 2022/11/15
 */
class UiObjectActionRegistry(id: Int) : AppletOptionRegistry(id) {

    private val swipeDirections by lazy {
        R.array.swipe_directions.array
    }

    private inline fun simpleUiObjectActionOption(
        title: Int, crossinline block: suspend (AccessibilityNodeInfo) -> Boolean
    ): AppletOption {
        return uiObjectActionOption<Unit>(title) { node, _ ->
            block(node)
        }
    }

    private inline fun <reified V> uiObjectActionOption(
        title: Int, crossinline block: suspend (AccessibilityNodeInfo, V?) -> Boolean
    ) = appletOption(title) {
        singleArgValueAction<AccessibilityNodeInfo, V> { node, value ->
            requireNotNull(node) {
                "Node is not captured?!"
            }
            node.ensureRefresh()
            block(node, value)
        }
    }

    @AppletOrdinal(0x0001)
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

    @AppletOrdinal(0x0002)
    val longClick = simpleUiObjectActionOption(R.string.format_perform_long_click) {
        it.ensureRefresh()
        if (it.isLongClickable) {
            it.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        } else {
            uiDevice.wrapUiObject(it).longClick()
            true
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object).hasCompositeTitle()

    @AppletOrdinal(0x00_03)
    val drag = uiObjectActionOption<Int>(R.string.format_drag) { node, v ->
        check(v != null)
        node.ensureRefresh()
        uiDevice.wrapUiObject(node).drag(IntValueUtil.parseXY(v))
        true
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object)
        .withUnaryArgument<Int>(
            R.string.specified_coordinate,
            variantType = VariantType.INT_COORDINATE
        )
        .withValueDescriber<Int> {
            val p = IntValueUtil.parseXY(it)
            R.string.format_coordinate.format(p.x, p.y)
        }
        .hasCompositeTitle()

    @AppletOrdinal(0x00_04)
    val swipe = uiObjectActionOption<Long>(R.string.format_swipe_ui_object) { node, v ->
        check(v != null)
        node.ensureRefresh()
        val swipe = SwipeMetrics.parse(v)
        uiDevice.wrapUiObject(node).swipe(swipe.direction, swipe.percent, swipe.speed)
        true
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object)
        .withValueArgument<Long>(R.string.swipe_args, VariantType.BITS_SWIPE)
        .withValueDescriber<Long> {
            val swipe = SwipeMetrics.parse(it)
            val direction = swipeDirections[Direction.ALL_DIRECTIONS.indexOf(swipe.direction)]
            R.string.format_swipe_args.format(direction, (swipe.percent * 100).toInt(), swipe.speed)
        }
        .hasCompositeTitle()

    @AppletOrdinal(0x0010)
    val setText = appletOption(R.string.format_perform_input_text) {
        referenceAction<String> { args, value, _ ->
            val node = args[0] as AccessibilityNodeInfo
            node.ensureRefresh()
            if (!node.isEditable) false else {
                uiDevice.wrapUiObject(node).setText(args[1]?.casted() ?: value)
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

    @AppletOrdinal(0x0020)
    val scrollForward = appletOption(R.string.scroll_list) {
        singleArgValueAction<AccessibilityNodeInfo, Long> { node, v ->
            node?.ensureRefresh()
            val metrics = ScrollMetrics.parse(v!!)
            if (metrics.direction == Direction.UP || metrics.direction == Direction.LEFT) {
                uiDevice.wrapUiScrollable(metrics.isVertical, node!!).scrollBackward(metrics.steps)
            } else {
                uiDevice.wrapUiScrollable(metrics.isVertical, node!!).scrollForward(metrics.steps)
            }
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.list)
        .withValueDescriber(scrollDescriber)
        .withValueArgument<Long>(R.string.swipe_args, VariantType.BITS_SCROLL)

    @AppletOrdinal(0x0021)
    val scrollToEnd = appletOption(R.string.scroll_to_end) {
        singleArgValueAction<AccessibilityNodeInfo, Long> { node, v ->
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
        .withValueDescriber(scrollDescriber)
        .withValueArgument<Long>(R.string.swipe_args, VariantType.BITS_SCROLL)

    @AppletOrdinal(0x0030)
    val scrollIntoUiObject = appletOption(R.string.scroll_into_ui_object) {
        ScrollIntoUiObject()
    }.withScopeRegistryId(BootstrapOptionRegistry.ID_UI_OBJECT_CRITERION_REGISTRY)
        .withRefArgument<AccessibilityNodeInfo>(R.string.list)
        .withValueArgument<Long>(R.string.swipe_args, VariantType.BITS_SCROLL)
        .withResult<AccessibilityNodeInfo>(R.string.matched_ui_object)
        .withResult<String>(R.string.ui_object_text)
        .withResult<Int>(R.string.ui_object_center_coordinate, VariantType.INT_COORDINATE)
        .withValueDescriber(scrollDescriber)
        .hasCompositeTitle()

    @AppletOrdinal(0x0040)
    val forEachUiScrollable = appletOption(R.string.for_each_ui_scrollable) {
        ForEachUiScrollable()
    }.withRefArgument<AccessibilityNodeInfo>(R.string.list)
        .withValueArgument<Long>(R.string.swipe_args, VariantType.BITS_SCROLL)
        .withResult<AccessibilityNodeInfo>(R.string.current_child)
        .withResult<Int>(R.string.current_child_count)
        .withValueDescriber(scrollDescriber)
        .hasCompositeTitle()

    @AppletOrdinal(0x0048)
    val getChildAt = appletOption(R.string.format_get_child_at) {
        LambdaReferenceAction<Int>(Applet.VAL_TYPE_INT) { args, value, _ ->
            val parent = args.single() as AccessibilityNodeInfo
            parent.ensureRefresh()
            val child = parent.getChild(value as Int - 1)
            if (child != null) {
                UiObjectReferent(child).asResult()
            } else {
                AppletResult.EMPTY_FAILURE
            }
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.parent_node, R.string.certain_ui_object)
        .withValueArgument<Int>(R.string.which_one)
        .withValueDescriber<Int> {
            R.string.format_number.formatSpans(it.toString().foreColored())
        }
        .withResult<AccessibilityNodeInfo>(R.string.child_node)
        .withResult<String>(R.string.ui_object_text)
        .hasCompositeTitle()

    @AppletOrdinal(0x0050)
    val drawNodeBounds = appletOption(R.string.format_draw_node_bounds) {
        singleArgAction<AccessibilityNodeInfo> {
            if (it != null) {
                currentService.overlayToastBridge.drawAccessibilityBounds(it)
                true
            } else {
                false
            }
        }
    }.withRefArgument<AccessibilityNodeInfo>(R.string.ui_object)
        .hasCompositeTitle()

    @AppletOrdinal(0x0051)
    val clearNodeBounds = appletOption(R.string.clear_node_bounds) {
        pureAction {
            currentService.overlayToastBridge.clearAccessibilityBounds()
        }
    }
}