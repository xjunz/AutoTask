package top.xjunz.tasker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.IAccessibilityServiceClient
import android.app.IUiAutomationConnection
import android.app.UiAutomation
import android.app.UiAutomationHidden
import android.app.accessibilityservice.AccessibilityServiceHidden
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.view.InputEvent
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.MutableLiveData
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.impl.A11yUiAutomatorBridge
import top.xjunz.tasker.impl.AvailabilityChecker
import top.xjunz.tasker.impl.IAvailabilityChecker
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.util.ReflectionUtil.requireFieldFromSuperClass
import top.xjunz.tasker.util.unsupportedOperation
import java.lang.ref.WeakReference

/**
 * @author xjunz 2022/07/12
 */
class A11yAutomatorService : AccessibilityService(), AutomatorService, IUiAutomationConnection {

    companion object {

        val error = MutableLiveData<Throwable>()

        val isRunning = MutableLiveData<Boolean>()

        private var instanceRef: WeakReference<A11yAutomatorService>? = null

        fun get() = instanceRef?.get()

        fun require() =
            checkNotNull(get()) { "The A11yAutomatorService is not yet started or is dead!" }
    }

    override val isRunning get() = Companion.isRunning.isTrue

    private var startTimestamp: Long = -1

    private var callbacks: AccessibilityServiceHidden.Callbacks? = null

    private lateinit var uiAutomationHidden: UiAutomationHidden

    private val _uiAutomation: UiAutomation by lazy {
        uiAutomationHidden.unsafeCast()
    }

    override val uiAutomatorBridge: UiAutomatorBridge by lazy {
        A11yUiAutomatorBridge(this, _uiAutomation)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            uiAutomationHidden = UiAutomationHidden(mainLooper, this)
            uiAutomationHidden.connect()
            instanceRef = WeakReference(this)
            startTimestamp = System.currentTimeMillis()
            Companion.isRunning.value = true
        } catch (t: Throwable) {
            t.printStackTrace()
            error.value = t
            destroy()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        callbacks?.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        TODO("interrupted!")
    }

    override fun onDestroy() {
        super.onDestroy()
        instanceRef?.clear()
        Companion.isRunning.value = false
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
        if (isRunning) uiAutomationHidden.disconnect()
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
}