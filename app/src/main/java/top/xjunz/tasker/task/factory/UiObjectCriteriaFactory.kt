package top.xjunz.tasker.task.factory

import android.content.res.Resources
import android.graphics.Rect
import android.view.Gravity
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.GravityInt
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.criterion.*
import top.xjunz.tasker.task.anno.AppletCategory
import top.xjunz.tasker.util.illegalArgument

/**
 * @author xjunz 2022/09/27
 */
class UiObjectCriteriaFactory(id: Int) : AppletFactory(id) {

    override val label: Int = R.string.ui_object_exists

    override val name: String = "UiObjectCriteriaFactory"

    private fun <V : Any> UiObjectCriterion(matcher: (AccessibilityNodeInfo, V) -> Boolean): Criterion<AccessibilityNodeInfo, V> {
        return BaseCriterion(matcher)
    }

    private fun UiObjectCheckCriterion(matcher: (AccessibilityNodeInfo) -> Boolean): Criterion<AccessibilityNodeInfo, Boolean> {
        return CheckCriteria(matcher)
    }

    private fun UiObjectBoundsCriterion(@GravityInt direction: Int): BoundsCriterion<AccessibilityNodeInfo> {
        return BoundsCriterion { node, scope, unit, portionScope ->
            val childRect = Rect()
            node.getBoundsInScreen(childRect)
            val delta = when (scope) {
                Distance.SCOPE_PARENT -> {
                    val parentRect = Rect()
                    node.parent.getBoundsInScreen(parentRect)
                    when (direction) {
                        Gravity.START -> childRect.left - parentRect.left
                        Gravity.END -> childRect.right - parentRect.right
                        Gravity.TOP -> childRect.top - parentRect.top
                        Gravity.BOTTOM -> childRect.bottom - parentRect.bottom
                        else -> illegalArgument("direction", direction)
                    }
                }
                Distance.SCOPE_SCREEN -> {
                    when (direction) {
                        Gravity.START -> childRect.left
                        Gravity.END -> childRect.right
                        Gravity.TOP -> childRect.top
                        Gravity.BOTTOM -> childRect.bottom
                        else -> illegalArgument("direction", direction)
                    }
                }
                else -> illegalArgument("scope", scope)
            }
            when (unit) {
                Distance.UNIT_DP -> {
                    delta / Resources.getSystem().displayMetrics.density
                }
                Distance.UNIT_PX -> delta.toFloat()
                Distance.UNIT_PORTION -> if (portionScope == Distance.SCOPE_SCREEN) {
                    childRect.width().toFloat() / Resources.getSystem().displayMetrics.widthPixels
                } else {
                    childRect.height().toFloat() / Resources.getSystem().displayMetrics.heightPixels
                }
                else -> illegalArgument("unit", unit)
            }
        }
    }

    // Type
    @AppletCategory(0x00_00)
    private val isType = AppletOption(0x0, R.string.is_type_of) {
        UiObjectCriterion<String> { t, v ->
            t.className == v
        }
    }

    // Text
    @AppletCategory(0x01_00)
    private val withText = AppletOption(0x10, R.string.with_text) {
        UiObjectCriterion<String> { t, v ->
            t.className == v
        }
    }

    @AppletCategory(0x01_01)
    private val textStartsWith = AppletOption(0x20, R.string.starts_with) {
        UiObjectCriterion<String> { t, v ->
            t.text.startsWith(v)
        }
    }

    @AppletCategory(0x01_02)
    private val textEndsWith = AppletOption(0x30, R.string.ends_with) {
        UiObjectCriterion<String> { t, v ->
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
        UiObjectCriterion<String> { t, v ->
            t.text.contains(v)
        }
    }

    @AppletCategory(0x01_05)
    private val textPattern = AppletOption(0x60, R.string.matches_pattern) {
        UiObjectCriterion<String> { t, v ->
            t.text.matches(Regex(v))
        }
    }

    @AppletCategory(0x01_06)
    private val contentDesc = NotInvertibleAppletOption(0x70, R.string.content_desc) {
        UiObjectCriterion<String> { t, v ->
            t.contentDescription == v
        }
    }

    @AppletCategory(0x02_00)
    private val isClickable = AppletOption(0x80, R.string.is_clickable) {
        UiObjectCheckCriterion {
            it.isClickable
        }
    }

    @AppletCategory(0x02_01)
    private val isLongClickable = AppletOption(0x81, R.string.is_long_clickable) {
        UiObjectCheckCriterion {
            it.isLongClickable
        }
    }

    @AppletCategory(0x02_02)
    private val isEditable = AppletOption(0x90, R.string.is_editable) {
        UiObjectCheckCriterion {
            it.isEditable
        }
    }

    @AppletCategory(0x02_03)
    private val isEnabled = AppletOption(0xA0, R.string.is_enabled) {
        UiObjectCheckCriterion {
            it.isEnabled
        }
    }

    @AppletCategory(0x02_04)
    private val isCheckable = AppletOption(0xB0, R.string.is_checkable) {
        UiObjectCheckCriterion {
            it.isCheckable
        }
    }

    @AppletCategory(0x02_05)
    private val isChecked = AppletOption(0xB1, R.string.is_checked) {
        UiObjectCheckCriterion {
            it.isChecked
        }
    }

    @AppletCategory(0x02_06)
    private val isSelected = AppletOption(0xC0, R.string.is_selected) {
        UiObjectCheckCriterion {
            it.isSelected
        }
    }

    @AppletCategory(0x0207)
    private val isScrollable = AppletOption(0xC0, R.string.is_scrollable) {
        UiObjectCheckCriterion {
            it.isScrollable
        }
    }


    override val categoryNames: IntArray
        get() = TODO("Not yet implemented")
}