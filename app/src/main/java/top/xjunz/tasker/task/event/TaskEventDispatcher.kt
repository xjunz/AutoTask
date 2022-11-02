package top.xjunz.tasker.task.event

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.accessibility.AccessibilityEvent
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.service.floatingInspector

/**
 * @author xjunz 2022/10/29
 */
class TaskEventDispatcher(looper: Looper, var callback: Callback) {

    private val activitySetCache = mutableSetOf<Int>()

    private var curEventTime: Long = -1
    private var curPackageName: String? = null
    private var curActivityName: String? = null
    private var prevContentChangedTime: Long = -1
    private var curPanelTitle: String? = null

    var contentChangedTimeout = 400

    private val handler = object : Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            callback.onEvent(msg.obj.casted())
        }
    }

    fun processAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (event.eventTime < curEventTime && !event.isFullScreen) return
        val className = event.className?.toString()
        if (className == "android.inputmethodservice.SoftInputWindow") return
        if (className == floatingInspector.getOverlayAccessibilityEventName()) return
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

                        val message = Message.obtain()
                        message.what = Event.EVENT_ON_CONTENT_CHANGED
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
                            message.obj = arrayOf(pkgEnterEvent, pkgExitEvent)
                        } else {
                            message.obj = arrayOf(pkgEnterEvent)
                        }
                        handler.sendMessage(message)
                        curPackageName = packageName
                        return
                    }
                }
                dispatchContentChanged(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                if (curEventTime - prevContentChangedTime >= contentChangedTimeout) {
                    prevContentChangedTime = curEventTime
                    dispatchContentChanged(packageName)
                }
            else -> dispatchContentChanged(packageName)
        }
    }

    private fun dispatchContentChanged(packageName: String) {
        if (packageName != curPackageName) return
        val message = Message.obtain()
        message.what = Event.EVENT_ON_CONTENT_CHANGED
        message.obj = arrayOf(
            Event.obtain(
                Event.EVENT_ON_CONTENT_CHANGED, packageName, curActivityName, curPanelTitle
            )
        )
        handler.sendMessage(message)
    }

    private fun isActivityExisting(pkgName: String, actName: String): Boolean {
        val hashCode = 31 * pkgName.hashCode() + actName.hashCode()
        if (activitySetCache.contains(hashCode)) return true
        if (activitySetCache.contains(-hashCode)) return false
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
            activitySetCache.add(hashCode)
        }.onFailure {
            activitySetCache.add(-hashCode)
        }.isSuccess
    }

    fun removePendingEvents() {
        handler.removeCallbacksAndMessages(null)
    }

    fun interface Callback {
        fun onEvent(events: Array<Event>)
    }
}