package top.xjunz.tasker.task.applet.option.registry

import android.graphics.Rect
import android.view.Gravity
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.GravityInt
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.BoundsCriterion
import top.xjunz.tasker.engine.applet.criterion.Criterion
import top.xjunz.tasker.engine.applet.criterion.NumberRangeCriterion
import top.xjunz.tasker.engine.applet.criterion.PropertyCriterion
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.value.Distance
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.flow.UiObjectContext
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.NotInvertibleAppletOption
import top.xjunz.tasker.util.illegalArgument

/**
 * @author xjunz 2022/09/27
 */
class UiObjectOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    private inline fun <V : Any> NodeCriterion(
        valueType: Int = AppletValues.VAL_TYPE_TEXT,
        crossinline matcher: (AccessibilityNodeInfo, V) -> Boolean
    ): Criterion<UiObjectContext, V> {
        return Criterion(valueType) { ctx, v ->
            matcher(ctx.source, v)
        }
    }

    private inline fun NodePropertyCriterion(crossinline matcher: (AccessibilityNodeInfo) -> Boolean)
            : PropertyCriterion<UiObjectContext> {
        return PropertyCriterion {
            matcher(it.source)
        }
    }

    private fun UiObjectBoundsCriterion(@GravityInt direction: Int): BoundsCriterion<UiObjectContext> {
        return BoundsCriterion(direction) { ctx, scope, unit ->
            val childRect = Rect()
            var parentRect: Rect? = null
            val node = ctx.source
            node.getBoundsInScreen(childRect)
            val delta = when (scope) {
                Distance.SCOPE_NONE -> {
                    when (direction) {
                        // Width
                        Gravity.FILL_HORIZONTAL -> childRect.width()
                        // Height
                        Gravity.FILL_VERTICAL -> childRect.height()
                        else -> illegalArgument("direction", direction)
                    }
                }
                Distance.SCOPE_PARENT -> {
                    parentRect = Rect()
                    node.parent.getBoundsInScreen(parentRect)
                    when (direction) {
                        Gravity.START -> childRect.left - parentRect.left
                        Gravity.END -> childRect.right - parentRect.right
                        Gravity.TOP -> childRect.top - parentRect.top
                        Gravity.BOTTOM -> childRect.bottom - parentRect.bottom
                        else -> illegalArgument("direction", direction)
                    }
                }
                Distance.SCOPE_SCREEN -> when (direction) {
                    Gravity.START -> childRect.left
                    Gravity.END -> childRect.right
                    Gravity.TOP -> childRect.top
                    Gravity.BOTTOM -> childRect.bottom
                    else -> illegalArgument("direction", direction)
                }
                else -> illegalArgument("scope", scope)
            }.toFloat()
            when (unit) {
                Distance.UNIT_DP -> delta / ctx.density
                Distance.UNIT_PX -> delta
                Distance.UNIT_SCREEN_WIDTH -> delta / ctx.screenWidthPixels
                Distance.UNIT_SCREEN_HEIGHT -> delta / ctx.screenHeightPixels
                Distance.UNIT_PARENT_WIDTH -> delta / parentRect!!.width()
                Distance.UNIT_PARENT_HEIGHT -> delta / parentRect!!.height()
                else -> illegalArgument("unit", unit)
            }
        }
    }

    private fun Float.toIntOrKeep(): Number {
        if (this % 1 == 0F) {
            return toInt()
        }
        return this
    }

    private val distanceDescriber = { d: Distance ->
        val start = d.rangeStart
        val stop = d.rangeEnd
        if (start == null && stop == null) {
            R.string.no_limit.str
        } else {
            val value = when {
                start == null && stop != null ->
                    R.string.format_less_than.format(stop.toIntOrKeep())
                stop == null && start != null ->
                    R.string.format_larger_than.format(start.toIntOrKeep())
                start == stop -> start!!.toIntOrKeep()
                else -> R.string.format_range.format(start!!.toIntOrKeep(), stop!!.toIntOrKeep())
            }
            val unit = when (d.unit) {
                Distance.UNIT_PX -> R.string.px.str
                Distance.UNIT_DP -> R.string.dip.str
                Distance.UNIT_SCREEN_WIDTH -> R.string.screen_width.str
                Distance.UNIT_SCREEN_HEIGHT -> R.string.screen_height.str
                Distance.UNIT_PARENT_WIDTH -> R.string.parent_width.str
                Distance.UNIT_PARENT_HEIGHT -> R.string.parent_height.str
                else -> illegalArgument()
            }
            if (d.scope == Distance.SCOPE_NONE) {
                "%s%s".format(value, unit)
            } else {
                val scope = when (d.scope) {
                    Distance.SCOPE_SCREEN -> R.string.screen.str
                    Distance.SCOPE_PARENT -> R.string.parent_node.str
                    else -> illegalArgument()
                }
                "%s%s(%s)".format(value, unit, R.string.relative_to.str + scope)
            }
        }
    }

    // Type
    @AppletCategory(0x00_00)
    val isType = NotInvertibleAppletOption(0x0, R.string.with_type) {
        NodeCriterion<String> { t, v ->
            t.className == v
        }
    }.withPresetArray(R.array.a11y_class_names, R.array.a11y_class_full_names)

    // ID
    @AppletCategory(0x00_01)
    val withId = NotInvertibleAppletOption(0x1, R.string.with_id) {
        NodeCriterion<String> { t, v ->
            t.viewIdResourceName == v
        }
    }

    // Text
    @AppletCategory(0x01_00)
    val textEquals = AppletOption(0x10, R.string.with_text) {
        NodeCriterion<String> { t, v ->
            t.className == v
        }
    }

    @AppletCategory(0x01_01)
    val textStartsWith = AppletOption(0x20, R.string.starts_with) {
        NodeCriterion<String> { t, v ->
            t.text.startsWith(v)
        }
    }

    @AppletCategory(0x01_02)
    val textEndsWith = AppletOption(0x30, R.string.ends_with) {
        NodeCriterion<String> { t, v ->
            t.text.endsWith(v)
        }
    }

    @AppletCategory(0x01_03)
    val textLengthRange = NotInvertibleAppletOption(0x40, R.string.in_length_range) {
        NumberRangeCriterion<AccessibilityNodeInfo, Int> {
            it.text.length
        }
    }

    @AppletCategory(0x01_04)
    val textContains = NotInvertibleAppletOption(0x50, R.string.contains_text) {
        NodeCriterion<String> { t, v ->
            t.text.contains(v)
        }
    }

    @AppletCategory(0x01_05)
    val textPattern = AppletOption(0x60, R.string.matches_pattern) {
        NodeCriterion<String> { t, v ->
            t.text.matches(Regex(v))
        }
    }

    @AppletCategory(0x01_06)
    val contentDesc = NotInvertibleAppletOption(0x70, R.string.content_desc) {
        NodeCriterion<String> { t, v ->
            t.contentDescription == v
        }
    }

    // Properties
    @AppletCategory(0x02_00)
    val isClickable = AppletOption(0x80, R.string.is_clickable) {
        NodePropertyCriterion {
            it.isClickable
        }
    }

    @AppletCategory(0x02_01)
    val isLongClickable = AppletOption(0x81, R.string.is_long_clickable) {
        NodePropertyCriterion {
            it.isLongClickable
        }
    }

    @AppletCategory(0x02_02)
    val isEditable = AppletOption(0x90, R.string.is_editable) {
        NodePropertyCriterion {
            it.isEditable
        }
    }

    @AppletCategory(0x02_03)
    val isEnabled = AppletOption(0xA0, R.string.is_enabled) {
        NodePropertyCriterion {
            it.isEnabled
        }
    }

    @AppletCategory(0x02_04)
    val isCheckable = AppletOption(0xB0, R.string.is_checkable) {
        NodePropertyCriterion {
            it.isCheckable
        }
    }

    @AppletCategory(0x02_05)
    val isChecked = AppletOption(0xB1, R.string.is_checked) {
        NodePropertyCriterion {
            it.isChecked
        }
    }

    @AppletCategory(0x02_06)
    val isSelected = AppletOption(0xC0, R.string.is_selected) {
        NodePropertyCriterion {
            it.isSelected
        }
    }

    @AppletCategory(0x0207)
    val isScrollable = AppletOption(0xD0, R.string.is_scrollable) {
        NodePropertyCriterion {
            it.isScrollable
        }
    }

    // Position
    @AppletCategory(0x0300)
    val left = NotInvertibleAppletOption(0xE0, R.string.left_margin) {
        UiObjectBoundsCriterion(Gravity.START)
    }.withDescriber(distanceDescriber)

    @AppletCategory(0x0301)
    val right = NotInvertibleAppletOption(0xE1, R.string.right_margin) {
        UiObjectBoundsCriterion(Gravity.END)
    }.withDescriber(distanceDescriber)

    @AppletCategory(0x0302)
    val top = NotInvertibleAppletOption(0xE2, R.string.top_margin) {
        UiObjectBoundsCriterion(Gravity.TOP)
    }.withDescriber(distanceDescriber)

    @AppletCategory(0x0303)
    val bottom = NotInvertibleAppletOption(0xE3, R.string.bottom_margin) {
        UiObjectBoundsCriterion(Gravity.BOTTOM)
    }.withDescriber(distanceDescriber)

    @AppletCategory(0x0304)
    val width = NotInvertibleAppletOption(0xE4, R.string.width) {
        UiObjectBoundsCriterion(Gravity.FILL_HORIZONTAL)
    }.withDescriber(distanceDescriber)

    @AppletCategory(0x0305)
    val height = NotInvertibleAppletOption(0xE5, R.string.height) {
        UiObjectBoundsCriterion(Gravity.FILL_VERTICAL)
    }.withDescriber(distanceDescriber)
/*
    @AppletCategory(0x0400)
    val depth = NotInvertibleAppletOption(ID_DEPTH, R.string.node_depth) {
        BaseCriterion<UiObjectContext, Int> { t, v ->

        }
    }*/

    override val title: Int = R.string.ui_object_exists

    override val categoryNames: IntArray =
        intArrayOf(R.string.type, R.string.text, R.string.property, R.string.position)
}