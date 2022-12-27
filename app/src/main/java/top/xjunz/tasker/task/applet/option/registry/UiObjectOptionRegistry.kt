package top.xjunz.tasker.task.applet.option.registry

import android.graphics.Rect
import android.view.Gravity
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.GravityInt
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.*
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.value.Distance
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.flow.UiObjectContext

/**
 * @author xjunz 2022/09/27
 */
class UiObjectOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    private inline fun <V : Any> nodeCriterion(
        valueType: Int = AppletValues.VAL_TYPE_TEXT,
        crossinline matcher: (AccessibilityNodeInfo, V) -> Boolean
    ): Criterion<UiObjectContext, V> {
        return LambdaCriterion(valueType) { ctx, v ->
            matcher(ctx.source, v)
        }
    }

    private inline fun nodePropertyCriterion(crossinline matcher: (AccessibilityNodeInfo) -> Boolean)
            : PropertyCriterion<UiObjectContext> {
        return PropertyCriterion {
            matcher(it.source)
        }
    }

    private fun uiObjectBoundsCriterion(@GravityInt direction: Int): BoundsCriterion<UiObjectContext> {
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
                start == stop -> R.string.format_equals_to.format(start!!.toIntOrKeep())
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
    val isType = appletOption(R.string.with_type) {
        nodeCriterion<String> { t, v ->
            t.className == v
        }
    }.withPresetArray(R.array.a11y_class_names, R.array.a11y_class_full_names)

    // ID
    @AppletCategory(0x00_01)
    val withId = appletOption(R.string.with_id) {
        nodeCriterion<String> { t, v ->
            t.viewIdResourceName == v
        }
    }

    // Text
    @AppletCategory(0x01_00)
    val textEquals = invertibleAppletOption(R.string.with_text) {
        nodeCriterion<String> { t, v ->
            t.className == v
        }
    }

    @AppletCategory(0x01_01)
    val textStartsWith = invertibleAppletOption(R.string.pkg_name_starts_with) {
        nodeCriterion<String> { t, v ->
            t.text?.startsWith(v) == true
        }
    }

    @AppletCategory(0x01_02)
    val textEndsWith = invertibleAppletOption(R.string.pkg_name_ends_with) {
        nodeCriterion<String> { t, v ->
            t.text?.endsWith(v) == true
        }
    }

    @AppletCategory(0x01_03)
    val textLengthRange = appletOption(R.string.in_length_range) {
        NumberRangeCriterion<AccessibilityNodeInfo, Int> {
            it.text?.length ?: -1
        }
    }.withDefaultRangeDescriber()

    @AppletCategory(0x01_04)
    val textContains = appletOption(R.string.contains_text) {
        nodeCriterion<String> { t, v ->
            t.text?.contains(v) == true
        }
    }

    @AppletCategory(0x01_05)
    val textPattern = invertibleAppletOption(R.string.pkg_name_matches_pattern) {
        nodeCriterion<String> { t, v ->
            t.text?.matches(Regex(v)) == true
        }
    }

    @AppletCategory(0x01_06)
    val contentDesc = appletOption(R.string.content_desc) {
        nodeCriterion<String> { t, v ->
            t.contentDescription == v
        }
    }.withTitleModifier("Content Description")

    // Properties
    @AppletCategory(0x02_00)
    val isClickable = invertibleAppletOption(R.string.is_clickable) {
        nodePropertyCriterion {
            it.isClickable
        }
    }

    @AppletCategory(0x02_01)
    val isLongClickable = invertibleAppletOption(R.string.is_long_clickable) {
        nodePropertyCriterion {
            it.isLongClickable
        }
    }

    @AppletCategory(0x02_02)
    val isEditable = invertibleAppletOption(R.string.is_editable) {
        nodePropertyCriterion {
            it.isEditable
        }
    }.withTitleModifier("Editable")

    @AppletCategory(0x02_03)
    val isEnabled = invertibleAppletOption(R.string.is_enabled) {
        nodePropertyCriterion {
            it.isEnabled
        }
    }.withTitleModifier("Enabled")

    @AppletCategory(0x02_04)
    val isCheckable = invertibleAppletOption(R.string.is_checkable) {
        nodePropertyCriterion {
            it.isCheckable
        }
    }.withTitleModifier("Checkable")

    @AppletCategory(0x02_05)
    val isChecked = invertibleAppletOption(R.string.is_checked) {
        nodePropertyCriterion {
            it.isChecked
        }
    }.withTitleModifier("Checked")

    @AppletCategory(0x02_06)
    val isSelected = invertibleAppletOption(R.string.is_selected) {
        nodePropertyCriterion {
            it.isSelected
        }
    }.withTitleModifier("Selected")

    @AppletCategory(0x0207)
    val isScrollable = invertibleAppletOption(R.string.is_scrollable) {
        nodePropertyCriterion {
            it.isScrollable
        }
    }.withTitleModifier("Scrollable")

    // Position
    @AppletCategory(0x0300)
    val left = appletOption(R.string.left_margin) {
        uiObjectBoundsCriterion(Gravity.START)
    }.withValueDescriber(distanceDescriber)

    @AppletCategory(0x0301)
    val right = appletOption(R.string.right_margin) {
        uiObjectBoundsCriterion(Gravity.END)
    }.withValueDescriber(distanceDescriber)

    @AppletCategory(0x0302)
    val top = appletOption(R.string.top_margin) {
        uiObjectBoundsCriterion(Gravity.TOP)
    }.withValueDescriber(distanceDescriber)

    @AppletCategory(0x0303)
    val bottom = appletOption(R.string.bottom_margin) {
        uiObjectBoundsCriterion(Gravity.BOTTOM)
    }.withValueDescriber(distanceDescriber)

    @AppletCategory(0x0304)
    val width = appletOption(R.string.width) {
        uiObjectBoundsCriterion(Gravity.FILL_HORIZONTAL)
    }.withValueDescriber(distanceDescriber)

    @AppletCategory(0x0305)
    val height = appletOption(R.string.height) {
        uiObjectBoundsCriterion(Gravity.FILL_VERTICAL)
    }.withValueDescriber(distanceDescriber)
/*
    @AppletCategory(0x0400)
    val depth = NotInvertibleAppletOption(ID_DEPTH, R.string.node_depth) {
        BaseCriterion<UiObjectContext, Int> { t, v ->

        }
    }*/

    override val categoryNames: IntArray =
        intArrayOf(R.string.type, R.string.text, R.string.property, R.string.position)
}