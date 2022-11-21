package top.xjunz.tasker.task.applet.option.registry

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.singleArgAction
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletOption

/**
 * @author xjunz 2022/11/15
 */
class UiObjectActionRegistry(id: Int) : AppletOptionRegistry(id) {

    override val categoryNames: IntArray? = null

    private inline fun simpleUiObjectActionOption(
        id: Int, title: Int, crossinline block: (AccessibilityNodeInfo) -> Boolean
    ): AppletOption {
        return uiObjectActionOption<Any>(id, title, AppletValues.VAL_TYPE_IRRELEVANT) { node, _ ->
            block(node)
        }
    }

    private inline fun <V> uiObjectActionOption(
        id: Int,
        title: Int,
        valueType: Int,
        crossinline block: (AccessibilityNodeInfo, V?) -> Boolean
    ): AppletOption {
        return appletOption(id, title) {
            singleArgAction<AccessibilityNodeInfo, V>(valueType) { node, value ->
                requireNotNull(node) {
                    "Node is not captured!"
                }
                if (node.refresh()) block(node, value) else false
            }
        }
    }

    @AppletCategory(0x0001)
    val click = simpleUiObjectActionOption(0x0001, R.string.perform_click) {
        if (it.isClickable) {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            uiDevice.wrapUiObject2(it).click()
            true
        }
    }.withArguments(R.string.ui_object to AccessibilityNodeInfo::class.java)

    @AppletCategory(0x0002)
    val longClick = simpleUiObjectActionOption(0x0002, R.string.perform_long_click) {
        if (it.isLongClickable) {
            it.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        } else {
            uiDevice.wrapUiObject2(it).longClick()
            true
        }
    }.withArguments(R.string.ui_object to AccessibilityNodeInfo::class.java)

    @AppletCategory(0x0010)
    val setText = uiObjectActionOption<String>(
        0x0010,
        R.string.perform_input_text,
        AppletValues.VAL_TYPE_TEXT
    ) { node, value ->
        if (!node.isEditable) false
        else {
            uiDevice.wrapUiObject2(node).text = value
            true
        }
    }.withArguments(
        R.string.input_field to AccessibilityNodeInfo::class.java,
        R.string.input_content to String::class.java
    )

}