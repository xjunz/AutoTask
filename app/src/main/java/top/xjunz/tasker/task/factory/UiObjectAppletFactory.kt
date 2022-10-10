package top.xjunz.tasker.task.factory

import android.graphics.Rect
import android.view.Gravity
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.GravityInt
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.criterion.*
import top.xjunz.tasker.task.anno.AppletCategory
import top.xjunz.tasker.task.flow.UiObjectContext
import top.xjunz.tasker.util.illegalArgument

/**
 * @author xjunz 2022/09/27
 */
class UiObjectAppletFactory(id: Int) : AppletFactory(id) {

    private inline fun <V : Any> NodeCriterion(crossinline matcher: (AccessibilityNodeInfo, V) -> Boolean): Criterion<UiObjectContext, V> {
        return BaseCriterion { ctx, v ->
            matcher(ctx.source, v)
        }
    }

    private inline fun NodePropertyCriterion(crossinline matcher: (AccessibilityNodeInfo) -> Boolean): Criterion<UiObjectContext, Boolean> {
        return PropertyCriterion {
            matcher(it.source)
        }
    }

    private fun UiObjectBoundsCriterion(@GravityInt direction: Int): BoundsCriterion<UiObjectContext> {
        return BoundsCriterion { ctx, scope, unit, portionScope ->
            val childRect = Rect()
            var parentRect: Rect? = null
            val node = ctx.source
            node.getBoundsInScreen(childRect)
            val horizontal = Gravity.isHorizontal(direction) || direction == Gravity.FILL_HORIZONTAL
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
                Distance.UNIT_PORTION -> if (portionScope == Distance.SCOPE_SCREEN) {
                    if (horizontal) delta / ctx.screenWidthPixels
                    else delta / ctx.screenHeightPixels
                } else {
                    if (horizontal) delta / parentRect!!.width()
                    else delta / parentRect!!.height()
                }
                else -> illegalArgument("unit", unit)
            }
        }
    }

    // Type
    @AppletCategory(0x00_00)
    private val isType = NotInvertibleAppletOption(0x0, R.string.is_type_of) {
        NodeCriterion<String> { t, v ->
            t.className == v
        }
    }

    // Text
    @AppletCategory(0x01_00)
    private val withText = AppletOption(0x10, R.string.with_text) {
        NodeCriterion<String> { t, v ->
            t.className == v
        }
    }

    @AppletCategory(0x01_01)
    private val textStartsWith = AppletOption(0x20, R.string.starts_with) {
        NodeCriterion<String> { t, v ->
            t.text.startsWith(v)
        }
    }

    @AppletCategory(0x01_02)
    private val textEndsWith = AppletOption(0x30, R.string.ends_with) {
        NodeCriterion<String> { t, v ->
            t.text.endsWith(v)
        }
    }

    @AppletCategory(0x01_03)
    private val textLengthRange = NotInvertibleAppletOption(0x40, R.string.in_length_range) {
        RangeCriterion<AccessibilityNodeInfo, Int> {
            it.text.length
        }
    }

    @AppletCategory(0x01_04)
    private val textContains = NotInvertibleAppletOption(0x50, R.string.contains_text) {
        NodeCriterion<String> { t, v ->
            t.text.contains(v)
        }
    }

    @AppletCategory(0x01_05)
    private val textPattern = AppletOption(0x60, R.string.matches_pattern) {
        NodeCriterion<String> { t, v ->
            t.text.matches(Regex(v))
        }
    }

    @AppletCategory(0x01_06)
    private val contentDesc = NotInvertibleAppletOption(0x70, R.string.content_desc) {
        NodeCriterion<String> { t, v ->
            t.contentDescription == v
        }
    }

    // Properties
    @AppletCategory(0x02_00)
    private val isClickable = AppletOption(0x80, R.string.is_clickable) {
        NodePropertyCriterion {
            it.isClickable
        }
    }

    @AppletCategory(0x02_01)
    private val isLongClickable = AppletOption(0x81, R.string.is_long_clickable) {
        NodePropertyCriterion {
            it.isLongClickable
        }
    }

    @AppletCategory(0x02_02)
    private val isEditable = AppletOption(0x90, R.string.is_editable) {
        NodePropertyCriterion {
            it.isEditable
        }
    }

    @AppletCategory(0x02_03)
    private val isEnabled = AppletOption(0xA0, R.string.is_enabled) {
        NodePropertyCriterion {
            it.isEnabled
        }
    }

    @AppletCategory(0x02_04)
    private val isCheckable = AppletOption(0xB0, R.string.is_checkable) {
        NodePropertyCriterion {
            it.isCheckable
        }
    }

    @AppletCategory(0x02_05)
    private val isChecked = AppletOption(0xB1, R.string.is_checked) {
        NodePropertyCriterion {
            it.isChecked
        }
    }

    @AppletCategory(0x02_06)
    private val isSelected = AppletOption(0xC0, R.string.is_selected) {
        NodePropertyCriterion {
            it.isSelected
        }
    }

    @AppletCategory(0x0207)
    private val isScrollable = AppletOption(0xD0, R.string.is_scrollable) {
        NodePropertyCriterion {
            it.isScrollable
        }
    }

    // Position
    @AppletCategory(0x0300)
    private val left = NotInvertibleAppletOption(0xE0, R.string.left_margin) {
        UiObjectBoundsCriterion(Gravity.START)
    }

    @AppletCategory(0x0301)
    private val right = NotInvertibleAppletOption(0xE1, R.string.right_margin) {
        UiObjectBoundsCriterion(Gravity.END)
    }

    @AppletCategory(0x0302)
    private val top = NotInvertibleAppletOption(0xE2, R.string.top_margin) {
        UiObjectBoundsCriterion(Gravity.TOP)
    }

    @AppletCategory(0x0303)
    private val bottom = NotInvertibleAppletOption(0xE3, R.string.bottom_margin) {
        UiObjectBoundsCriterion(Gravity.BOTTOM)
    }

    @AppletCategory(0x0304)
    private val width = NotInvertibleAppletOption(0xE4, R.string.width) {
        UiObjectBoundsCriterion(Gravity.FILL_HORIZONTAL)
    }

    @AppletCategory(0x0305)
    private val height = NotInvertibleAppletOption(0xE5, R.string.height) {
        UiObjectBoundsCriterion(Gravity.FILL_VERTICAL)
    }

    override val title: Int = R.string.ui_object_exists

    override val categoryNames: IntArray = intArrayOf(R.string.type, R.string.text, R.string.property, R.string.position)
}