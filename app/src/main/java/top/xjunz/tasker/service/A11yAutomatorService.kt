package top.xjunz.tasker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.IAccessibilityServiceClient
import android.app.IUiAutomationConnection
import android.app.UiAutomation
import android.app.UiAutomationHidden
import android.app.accessibilityservice.AccessibilityServiceHidden
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.view.InputEvent
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.impl.A11yUiAutomatorBridge
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.task.inspector.ComponentInfo
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorViewModel
import top.xjunz.tasker.util.ReflectionUtil.requireFieldFromSuperClass
import top.xjunz.tasker.util.unsupportedOperation
import java.lang.ref.WeakReference

/**
 * @author xjunz 2022/07/12
 */
class A11yAutomatorService : AccessibilityService(), AutomatorService, IUiAutomationConnection,
    LifecycleOwner {

    companion object {

        var FLAG_REQUEST_INSPECTOR_MODE: Boolean = false

        val launchError = MutableLiveData<Throwable>()

        val runningState = MutableLiveData<Boolean>()

        private var instanceRef: WeakReference<A11yAutomatorService>? = null

        fun get() = instanceRef?.get()

        fun require() = checkNotNull(get()) {
            "The A11yAutomatorService is not yet started or is dead!"
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)

    private lateinit var inspectorViewModel: InspectorViewModel

    override val isRunning get() = runningState.isTrue

    private var startTimestamp: Long = -1

    private var callbacks: AccessibilityServiceHidden.Callbacks? = null

    private lateinit var uiAutomationHidden: UiAutomationHidden

    private val uiAutomation: UiAutomation get() = uiAutomationHidden.casted()

    lateinit var inspector: FloatingInspector

    override val uiAutomatorBridge: UiAutomatorBridge by lazy {
        A11yUiAutomatorBridge(this, uiAutomation)
    }

    private var launchedInInspectorMode = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            launchedInInspectorMode = FLAG_REQUEST_INSPECTOR_MODE
            if (!launchedInInspectorMode) {
                uiAutomationHidden = UiAutomationHidden(mainLooper, this)
                uiAutomationHidden.connect()
            }
            instanceRef = WeakReference(this)
            runningState.value = true
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            startTimestamp = System.currentTimeMillis()
        } catch (t: Throwable) {
            t.printStackTrace()
            launchError.value = t
            destroy()
        }
    }

    fun destroyFloatingInspector() {
        inspector.dismiss()
        if (launchedInInspectorMode) {
            disableSelf()
        }
    }

    fun isInspectorShown(): Boolean {
        return ::inspector.isInitialized && inspector.isShown
    }

    fun showFloatingInspector() {
        if (isInspectorShown()) return
        inspectorViewModel = InspectorViewModel()
        inspector = FloatingInspector(this, inspectorViewModel)
        inspector.show()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isInspectorShown()) {
            // recreate the inspector
            inspector.dismiss()
            inspectorViewModel.onConfigurationChanged()
            inspector = FloatingInspector(this, inspectorViewModel)
            inspector.show()
        }
    }

    private var prevComp: ComponentInfo? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (isInspectorShown() && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val actName = event.className?.toString() ?: return
            val pkgName = event.packageName.toString()
            var actLabel: String? = null
            if (event.text.size > 0) {
                if (event.text[0] != null) actLabel = event.text[0].toString()
            }
            val currentComp = ComponentInfo(actLabel, pkgName, actName)
            if (currentComp != prevComp && currentComp.isActivity()) {
                inspectorViewModel.currentComp.value = currentComp
                prevComp = currentComp
            }
        }
        callbacks?.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {

    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        instanceRef?.clear()
        runningState.value = false
        if (isInspectorShown()) inspector.dismiss()
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
        callbacks!!.init(connectionId, windowToken)
    }

    override fun disconnect() {
        destroy()
    }

    override fun destroy() {
        if (isRunning && !launchedInInspectorMode) {
            uiAutomationHidden.disconnect()
        }
        disableSelf()
    }

    override fun getStartTimestamp(): Long {
        return startTimestamp
    }

    override fun createAvailabilityChecker(): IAvailabilityChecker {
        return AvailabilityChecker(this)
    }

    override fun setRotation(rotation: Int): Boolean {
        unsupportedOperation()
    }

    override fun takeScreenshot(crop: Rect, rotation: Int): Bitmap {
        unsupportedOperation()
    }

    override fun executeShellCommand(
        command: String?,
        sink: ParcelFileDescriptor?,
        source: ParcelFileDescriptor?
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

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}