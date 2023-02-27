/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.inspector

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.databinding.DialogFloatingInspectorBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.service.a11yAutomatorService
import top.xjunz.tasker.service.controller.A11yAutomatorServiceController
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.service.isFloatingInspectorShown
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.ShizukuUtil

/**
 * @author xjunz 2022/10/09
 */
class FloatingInspectorDialog : BaseBottomSheetDialog<DialogFloatingInspectorBinding>() {

    private lateinit var overlaySettingLauncher: ActivityResultLauncher<Intent>

    private lateinit var accessibilitySettingsLauncher: ActivityResultLauncher<Intent>

    private class InnerViewModel : ViewModel() {

        var checkedViewId: Int = R.id.rb_mode_shizuku

        val isBinding = MutableLiveData<Boolean>()

        val onError = MutableLiveData<Throwable>()

        var mode = InspectorMode.COMPONENT

        var doOnSucceeded: Runnable? = null

        fun enableA11yServiceRemoteExecCmd() {
            // TODO: not working on low api devices ->
            // TODO: SystemServiceHelper.getSystemService("settings") returns null
            val className = "${BuildConfig.APPLICATION_ID}/${A11yAutomatorService::class.java.name}"
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("settings"))
                .execShellCmd("put", "secure", "enabled_accessibility_services", className)
        }

        fun enableA11yService() {
            viewModelScope.launch {
                try {
                    isBinding.value = true
                    enableA11yServiceRemoteExecCmd()
                    // Wait for the service to be pulled up
                    val start = SystemClock.uptimeMillis()
                    while (A11yAutomatorService.get() == null
                        && SystemClock.uptimeMillis() - start <= 1000
                    ) {
                        delay(50)
                    }
                } catch (t: Throwable) {
                    onError.value = t
                }
                isBinding.value = false
            }
        }
    }

    private val viewModel by viewModels<InnerViewModel>()

    private fun showInspectorAndDismissSelf() {
        if (isFloatingInspectorShown) {
            if (floatingInspector.mode == viewModel.mode) {
                toast(R.string.tip_floating_inspector_enabled)
            } else {
                toast(R.string.format_switch_mode.format(viewModel.mode.label))
            }
        } else {
            toast(R.string.tip_floating_inspector_enabled)
        }
        a11yAutomatorService.showFloatingInspector(viewModel.mode)
        if (viewModel.mode == InspectorMode.COMPONENT) {
            a11yAutomatorService.startListeningComponentChanges()
        }
        viewModel.doOnSucceeded?.run()
        dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlaySettingLauncher = registerForActivityResult(StartActivityForResult()) {
            updateOverlayGrantButton()
            if (FloatingInspector.isReady())
                showInspectorAndDismissSelf()
            if (!Settings.canDrawOverlays(app))
                toast(R.string.grant_failed)
        }
        accessibilitySettingsLauncher = registerForActivityResult(StartActivityForResult()) {
            if (FloatingInspector.isReady())
                showInspectorAndDismissSelf()
            if (A11yAutomatorService.get() == null)
                toast(R.string.grant_failed)
        }
    }

    fun doOnSucceeded(block: Runnable) = doWhenCreated {
        viewModel.doOnSucceeded = block
    }

    fun setMode(mode: InspectorMode) = doWhenCreated {
        viewModel.mode = mode
    }

    private fun <T> launchIntentSafely(launcher: ActivityResultLauncher<T>, param: T) =
        runCatching {
            launcher.launch(param)
        }.isSuccess


    private fun launchOverlaySettings() {
        toast(R.string.pls_enable_overlay_manually)
        launchIntentSafely(
            overlaySettingLauncher,
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .setData(Uri.parse("package:${BuildConfig.APPLICATION_ID}"))
        )
    }

    private fun launchAccessibilitySettings() {
        toast(R.string.pls_start_a11y_service_manually)
        launchIntentSafely(
            accessibilitySettingsLauncher, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        )
    }

    private fun updateOverlayGrantButton() {
        if (Settings.canDrawOverlays(app)) {
            binding.btnGrant.isEnabled = false
            binding.btnGrant.text = R.string.granted.text
            binding.btnGrant.icon = null
        } else {
            binding.btnGrant.isEnabled = true
            binding.btnGrant.text = R.string.goto_grant.text
            binding.btnGrant.icon = R.drawable.ic_chevron_right_24px.getDrawable()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (FloatingInspector.isReady()) {
            showInspectorAndDismissSelf()
            return null
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        A11yAutomatorService.FLAG_REQUEST_INSPECTOR_MODE = true
        updateOverlayGrantButton()
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        binding.rgModes.check(viewModel.checkedViewId)
        binding.rgModes.setOnCheckedChangeListener { _, checkedId ->
            viewModel.checkedViewId = checkedId
        }
        binding.btnGrant.setNoDoubleClickListener {
            launchOverlaySettings()
        }
        binding.btnEnable.setNoDoubleClickListener {
            if (!Settings.canDrawOverlays(app)) {
                launchOverlaySettings()
            } else {
                if (viewModel.checkedViewId == R.id.rb_mode_shizuku) {
                    ShizukuUtil.ensureShizukuEnv {
                        viewModel.enableA11yService()
                    }
                } else if (viewModel.checkedViewId == R.id.rb_mode_a11y) {
                    launchAccessibilitySettings()
                }
            }
        }
        observeError(viewModel.onError)
        var progress: AlertDialog? = null
        observeNotNull(viewModel.isBinding) {
            if (it) {
                progress = makeProgressDialog().show()
            } else {
                progress?.dismiss()
                if (A11yAutomatorServiceController.isServiceRunning) {
                    // Successfully enabled
                    viewModel.isBinding.value = null
                    if (FloatingInspector.isReady()) {
                        showInspectorAndDismissSelf()
                        return@observeNotNull
                    }
                } else {
                    toast(R.string.launch_failed)
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        A11yAutomatorService.FLAG_REQUEST_INSPECTOR_MODE = false
    }
}