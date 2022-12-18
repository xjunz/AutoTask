package top.xjunz.tasker.task.event

import android.app.Notification
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.service.isFloatingInspectorShown
import java.lang.ref.WeakReference

/**
 * @author xjunz 2022/10/29
 */
class A11yEventDispatcher(callback: Callback) : EventDispatcher(callback) {

    private val activityHashCache = mutableSetOf<Int>()

    private var curEventTime: Long = -1
    private var curPackageName: String? = null
    private var curActivityName: String? = null
    private var curPanelTitle: String? = null

    var contentChangedTimeout = 250L

    private var eventDispatchScope: WeakReference<CoroutineScope>? = null

    suspend fun processAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (event.eventTime < curEventTime && !event.isFullScreen) return
        val className = event.className?.toString()
        if (className == "android.inputmethodservice.SoftInputWindow") return
        if (isFloatingInspectorShown && className == floatingInspector.exemptionEventClassName) return
        curEventTime = event.eventTime
        val firstText = event.text.firstOrNull()?.toString()
        val prevPanelTitle = curPanelTitle
        if (firstText != null
            && event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            && event.contentChangeTypes != AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
            && packageName == curPackageName
        )
            curPanelTitle = firstText

        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                if (className == Notification::class.java.name) {
                    dispatchEvents(
                        Event.obtain(
                            Event.EVENT_ON_NOTIFICATION_RECEIVED,
                            packageName, paneTitle = firstText
                        )
                    )
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {

                val prevActivityName = curActivityName
                val isActivity = className != null && className != curActivityName
                        && isActivityExisting(packageName, className)

                if (isActivity)
                    curActivityName = className

                if (
                    event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    // Only full screen windows, because there may be overlay windows
                    && event.isFullScreen
                ) {
                    curPanelTitle = firstText
                    if (curPackageName != packageName) {
                        if (!isActivity)
                            curActivityName = null

                        val pkgEnterEvent = Event.obtain(
                            Event.EVENT_ON_PACKAGE_ENTERED,
                            packageName,
                            curActivityName,
                            curPanelTitle
                        )
                        if (curPackageName != null) {
                            val pkgExitEvent = Event.obtain(
                                Event.EVENT_ON_PACKAGE_EXITED,
                                curPackageName!!,
                                prevActivityName,
                                prevPanelTitle
                            )
                            dispatchEvents(pkgEnterEvent, pkgExitEvent)
                        } else {
                            dispatchEvents(pkgEnterEvent)
                        }
                        curPackageName = packageName
                        return
                    }
                }
                dispatchContentChanged(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                eventDispatchScope?.get()?.cancel()
                coroutineScope {
                    eventDispatchScope = WeakReference(this)
                    delay(contentChangedTimeout)
                    dispatchContentChanged(packageName)
                }
            }
            else -> dispatchContentChanged(packageName)
        }
    }

    private fun dispatchContentChanged(packageName: String) {
        if (packageName != curPackageName) return
        dispatchEvents(
            Event.obtain(
                Event.EVENT_ON_CONTENT_CHANGED,
                packageName, curActivityName, curPanelTitle
            )
        )
    }

    private fun isActivityExisting(pkgName: String, actName: String): Boolean {
        val hashCode = 31 * pkgName.hashCode() + actName.hashCode()
        if (activityHashCache.contains(hashCode)) return true
        if (activityHashCache.contains(-hashCode)) return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.packageManager.getActivityInfo(
                    ComponentName(pkgName, actName), PackageManager.ComponentInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getActivityInfo(ComponentName(pkgName, actName), 0)
            }
        }.onSuccess {
            activityHashCache.add(hashCode)
        }.onFailure {
            activityHashCache.add(-hashCode)
        }.isSuccess
    }
}