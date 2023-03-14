/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceHidden
import android.accessibilityservice.IAccessibilityServiceClient
import android.app.IUiAutomationConnection
import android.app.UiAutomation
import android.app.UiAutomationHidden
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.InputEvent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.*
import top.xjunz.shared.ktx.casted
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.bridge.A11yUiAutomatorBridge
import top.xjunz.tasker.bridge.OverlayToastBridge
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.event.A11yEventDispatcher
import top.xjunz.tasker.task.event.ClipboardEventDispatcher
import top.xjunz.tasker.task.event.MetaEventDispatcher
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.task.inspector.InspectorViewModel
import top.xjunz.tasker.task.runtime.ITaskCompletionCallback
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.task.runtime.OneshotTaskScheduler
import top.xjunz.tasker.task.runtime.ResidentTaskScheduler
import top.xjunz.tasker.uiautomator.A11yGestureController
import top.xjunz.tasker.util.ReflectionUtil.requireFieldFromSuperClass
import java.lang.ref.WeakReference

/**
 * @see ShizukuAutomatorService
 *
 * @author xjunz 2022/07/12
 */
class A11yAutomatorService : AccessibilityService(), AutomatorService, IUiAutomationConnection,
    LifecycleOwner, LifecycleEventObserver {

    companion object {

        val LAUNCH_ERROR = MutableLiveData<Throwable?>()

        val RUNNING_STATE = MutableLiveData<Boolean>()

        /**
         * Indicate that the service is served as [FloatingInspector].
         */
        var FLAG_REQUEST_INSPECTOR_MODE: Boolean = true

        private var instance: WeakReference<A11yAutomatorService>? = null

        fun get() = instance?.get()

        fun require() = checkNotNull(get()) {
            "The A11yAutomatorService is not yet started or is dead!"
        }
    }

    val isInspectorShown get() = ::inspector.isInitialized && inspector.isShown

    val gestureController by lazy {
        uiAutomatorBridge.gestureController as A11yGestureController
    }

    private val lifecycleRegistry = LifecycleRegistry(this)

    private val uiAutomation: UiAutomation get() = uiAutomationHidden.casted()

    private val residentTaskScheduler = ResidentTaskScheduler(LocalTaskManager)

    private val oneshotTaskScheduler = OneshotTaskScheduler()

    private val componentChangeCallbackForInspector by lazy {
        EventDispatcher.Callback {
            val hit = it.find { event ->
                event.type == Event.EVENT_ON_CONTENT_CHANGED || event.type == Event.EVENT_ON_PACKAGE_ENTERED
            }
            if (hit != null) {
                inspectorViewModel.currentComponent.postValue(
                    ComponentInfoWrapper.wrap(hit.componentInfo)
                )
            }
        }
    }

    override val isRunning get() = RUNNING_STATE.isTrue

    override val uiAutomatorBridge by lazy {
        A11yUiAutomatorBridge(uiAutomation)
    }

    override val eventDispatcher = MetaEventDispatcher()

    private val a11yEventDispatcher: A11yEventDispatcher by lazy {
        A11yEventDispatcher(Looper.getMainLooper(), uiAutomatorBridge)
    }

    private val overlayToastBridgeLazy = lazy {
        OverlayToastBridge(Looper.getMainLooper())
    }

    override val overlayToastBridge: OverlayToastBridge by overlayToastBridgeLazy

    private var startTimestamp: Long = -1

    private var callbacks: AccessibilityServiceHidden.Callbacks? = null

    lateinit var inspector: FloatingInspector
        private set

    var isInspectorMode = false
        private set

    private lateinit var uiAutomationHidden: UiAutomationHidden

    private lateinit var inspectorViewModel: InspectorViewModel

    fun startListeningComponentChanges() {
        a11yEventDispatcher.addCallback(componentChangeCallbackForInspector)
    }

    private fun stopListeningComponentChanges() {
        a11yEventDispatcher.removeCallback(componentChangeCallbackForInspector)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            isInspectorMode = FLAG_REQUEST_INSPECTOR_MODE
            instance = WeakReference(this)

            uiAutomationHidden = UiAutomationHidden(mainLooper, this)
            uiAutomationHidden.connect()

            if (!isInspectorMode) {
                initTaskScheduler()
            }
            a11yEventDispatcher.activate(isInspectorMode)

            lifecycleRegistry.addObserver(this)
            lifecycleRegistry.currentState = Lifecycle.State.STARTED

            startTimestamp = System.currentTimeMillis()
        } catch (t: Throwable) {
            t.logcatStackTrace()
            LAUNCH_ERROR.value = t
            destroy()
        } finally {
            FLAG_REQUEST_INSPECTOR_MODE = true
        }
    }

    private fun initTaskScheduler() {
        eventDispatcher.registerEventDispatcher(a11yEventDispatcher)
        eventDispatcher.registerEventDispatcher(ClipboardEventDispatcher())
        eventDispatcher.addCallback(residentTaskScheduler)
        eventDispatcher.addCallback(oneshotTaskScheduler)
    }

    fun destroyFloatingInspector() {
        inspector.dismiss()
        stopListeningComponentChanges()
        if (isInspectorMode) {
            disableSelf()
        } else if (serviceController.isServiceRunning) {
            currentService.suppressResidentTaskScheduler(false)
        }
    }

    fun switchToWorkerMode() {
        isInspectorMode = false
        initTaskScheduler()
    }

    fun showFloatingInspector(mode: InspectorMode) {
        if (isInspectorShown) {
            if (inspector.mode != mode) {
                // Collapse first to dodge complex conditions
                inspectorViewModel.isCollapsed.value = true
                inspectorViewModel.currentMode.value = mode
            }
        } else {
            inspectorViewModel = InspectorViewModel()
            inspectorViewModel.currentMode.value = mode
            inspector = FloatingInspector(this, inspectorViewModel)
            inspector.show()
        }
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun suppressResidentTaskScheduler(suppress: Boolean) {
        residentTaskScheduler.isSuppressed = suppress
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isInspectorShown) {
            // Recreate the inspector
            inspector.dismiss()
            inspectorViewModel.onConfigurationChanged()
            inspector = FloatingInspector(this, inspectorViewModel)
            inspector.show()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        callbacks?.onAccessibilityEvent(event)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (isInspectorShown && event.action == KeyEvent.ACTION_UP) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK
                || event.keyCode == KeyEvent.KEYCODE_HOME
                || event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
            ) {
                inspectorViewModel.recordKeyEvent(event.keyCode)
            }
        }
        return super.onKeyEvent(event)
    }

    override fun scheduleOneshotTask(task: XTask, onCompletion: ITaskCompletionCallback) {
        oneshotTaskScheduler.scheduleTask(task, onCompletion)
    }

    override fun stopOneshotTask(task: XTask) {
        task.halt()
    }

    override fun onInterrupt() {

    }

    override fun onDestroy() {
        super.onDestroy()
        eventDispatcher.destroy()
        if (overlayToastBridgeLazy.isInitialized()) {
            overlayToastBridge.destroy()
        }
        residentTaskScheduler.shutdown()
        oneshotTaskScheduler.shutdown()
        LocalTaskManager.clearAllSnapshots()
        if (isInspectorShown) {
            inspector.dismiss()
        }
        if (::uiAutomationHidden.isInitialized) {
            uiAutomationHidden.disconnect()
        }
        instance?.clear()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    /**
     * Connect to the [UiAutomation] with limited features in a delicate way. Should take this
     * really carefully. After connected, we can use [UiAutomation.setOnAccessibilityEventListener],
     * [UiAutomation.waitForIdle] and [UiAutomation.executeAndWaitForEvent].
     *
     * This method will be called on [UiAutomationHidden.connect].
     */
    override fun connect(client: IAccessibilityServiceClient, flags: Int) {
        val windowToken: IBinder = requireFieldFromSuperClass("mWindowToken")
        val connectionId: Int = requireFieldFromSuperClass("mConnectionId")
        callbacks = client.requireFieldFromSuperClass("mCallback")
        requireNotNull(callbacks).init(connectionId, windowToken)
    }

    override fun disconnect() {
        destroy()
    }

    override fun destroy() {
        disableSelf()
    }

    override fun getStartTimestamp(): Long {
        return startTimestamp
    }

    override fun setRotation(rotation: Int): Boolean {
        unsupportedOperation()
    }

    override fun takeScreenshot(crop: Rect, rotation: Int): Bitmap {
        unsupportedOperation()
    }

    override fun executeShellCommand(
        command: String?, sink: ParcelFileDescriptor?, source: ParcelFileDescriptor?
    ) {
        unsupportedOperation()
    }

    override fun shutdown() {
        destroy()
    }

    override fun asBinder(): IBinder {
        unsupportedOperation()
    }

    override fun injectInputEvent(event: InputEvent?, sync: Boolean): Boolean {
        unsupportedOperation()
    }

    override fun syncInputTransactions() {
        unsupportedOperation()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_START) {
            RUNNING_STATE.value = true
        } else if (event == Lifecycle.Event.ON_DESTROY) {
            RUNNING_STATE.value = false
            lifecycleRegistry.removeObserver(this)
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}