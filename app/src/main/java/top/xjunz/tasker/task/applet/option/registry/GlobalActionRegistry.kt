package top.xjunz.tasker.task.applet.option.registry

import android.accessibilityservice.AccessibilityService
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.ActivityManagerBridge
import top.xjunz.tasker.bridge.ClipboardManagerBridge
import top.xjunz.tasker.engine.applet.action.*
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.ktx.firstGroupValue
import top.xjunz.tasker.service.uiAutomation
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletOption

/**
 * @author xjunz 2022/11/15
 */
class GlobalActionRegistry(id: Int) : AppletOptionRegistry(id) {

    override val categoryNames: IntArray? = null

    private fun globalActionOption(id: Int, title: Int, action: Int): AppletOption {
        return appletOption(id, title) {
            simpleAction {
                uiAutomation.performGlobalAction(action)
            }
        }
    }

    @AppletCategory(0x0000)
    val pressBack = globalActionOption(
        0x0000, R.string.press_back, AccessibilityService.GLOBAL_ACTION_BACK
    )

    @AppletCategory(0x0001)
    val pressRecents = globalActionOption(
        0x0001, R.string.press_recent, AccessibilityService.GLOBAL_ACTION_RECENTS
    )

    @AppletCategory(0x0002)
    val pressHome = globalActionOption(
        0x0002, R.string.press_home, AccessibilityService.GLOBAL_ACTION_HOME
    )

    @AppletCategory(0x0003)
    val forceStop = appletOption(0x0010, R.string.force_stop_current_pkg) {
        pureAction {
            ActivityManagerBridge.forceStopPackage(it.hitEvent.componentInfo.pkgName)
        }
    }.shizukuOnly()

    @AppletCategory(0x0004)
    val extractText = appletOption(0x0020, R.string.format_extract_text) {
        unaryArgProcessor<String, String>(AppletValues.VAL_TYPE_TEXT) { arg, v ->
            if (v == null) null else arg?.firstGroupValue(v)
        }
    }.withRefArgument<String>(R.string.text)
        .withValueArgument<String>(R.string.regex)
        .withResult<String>(R.string.extracted_text)
        .withHelperText(R.string.help_extract_text)
        .hasCompositeTitle()

    @AppletCategory(0x0005)
    val copyText = appletOption(0x0021, R.string.format_copy_text) {
        singleArgAction<String, String>(AppletValues.VAL_TYPE_TEXT) { arg, v ->
            if (arg == null && v == null) {
                false
            } else if (arg != null) {
                ClipboardManagerBridge.copyToClipboard(arg)
                true
            } else if (v != null) {
                ClipboardManagerBridge.copyToClipboard(v)
                true
            } else {
                false
            }
        }
    }.withArgument<String>(R.string.text)
        .hasCompositeTitle()
}