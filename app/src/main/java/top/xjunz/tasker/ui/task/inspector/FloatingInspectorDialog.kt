package top.xjunz.tasker.ui.task.inspector

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.databinding.DialogFloatingInspectorBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.service.a11yAutomatorService
import top.xjunz.tasker.service.controller.A11yAutomatorServiceController
import top.xjunz.tasker.service.controller.ServiceController
import top.xjunz.tasker.service.controller.ShizukuA11yServiceEnabler
import top.xjunz.tasker.service.isFloatingInspectorShown
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.MainViewModel
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog

/**
 * @author xjunz 2022/10/09
 */
class FloatingInspectorDialog : BaseBottomSheetDialog<DialogFloatingInspectorBinding>() {

    private lateinit var overlaySettingLauncher: ActivityResultLauncher<Intent>

    private lateinit var accessibilitySettingsLauncher: ActivityResultLauncher<Intent>

    private class InnerViewModel : ViewModel(), ServiceController.ServiceStateListener {

        val enabler = ShizukuA11yServiceEnabler()

        var checkedViewId: Int = R.id.rb_mode_shizuku

        val isBinding = MutableLiveData<Boolean>()

        val onError = MutableLiveData<Throwable>()

        var mode = InspectorMode.COMPONENT

        override fun onStartBinding() {
            isBinding.postValue(true)
        }

        override fun onError(t: Throwable) {
            onError.postValue(t)
            isBinding.postValue(false)
        }

        override fun onServiceBound() {
            enableA11yService()
        }

        override fun onServiceDisconnected() {
        }

        fun connectToEnabler() {
            if (!A11yAutomatorServiceController.isServiceRunning) {
                enabler.setStateListener(this)
                enabler.bindService()
            } else {
                isBinding.postValue(false)
            }
        }

        private fun enableA11yService() {
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    enabler.enableA11yService(true)
                    // Delay 500ms to wait for the a11y service to be pulled up
                    delay(500)
                } catch (t: Throwable) {
                    onError.postValue(t)
                }
                isBinding.postValue(false)
            }
        }

        override fun onCleared() {
            super.onCleared()
            enabler.unbindService()
        }
    }

    private val viewModel by viewModels<InnerViewModel>()

    private lateinit var mainViewModel: MainViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainViewModel = requireActivity().viewModels<MainViewModel>().value
    }

    private fun showInspectorAndDismissSelf() {
        a11yAutomatorService.showFloatingInspector(viewModel.mode)
        dismiss()
        toast(R.string.tip_floating_inspector_enabled, Toast.LENGTH_LONG)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlaySettingLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                updateOverlayGrantButton()
                if (FloatingInspector.isReady()) {
                    showInspectorAndDismissSelf()
                } else {
                    toast(R.string.grant_failed)
                }
            }
        accessibilitySettingsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (FloatingInspector.isReady()) {
                    showInspectorAndDismissSelf()
                } else {
                    toast(R.string.grant_failed)
                }
            }
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
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).setData(Uri.parse("package:${BuildConfig.APPLICATION_ID}"))
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
            binding.btnGrant.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_chevron_right_24)
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
        binding.btnGrant.setOnClickListener {
            launchOverlaySettings()
        }
        binding.btnEnable.setOnClickListener {
            if (!Settings.canDrawOverlays(app)) {
                launchOverlaySettings()
            } else {
                if (viewModel.checkedViewId == R.id.rb_mode_shizuku) {
                    viewModel.connectToEnabler()
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
                }
                toast(R.string.launch_failed)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isFloatingInspectorShown) {
            A11yAutomatorService.FLAG_REQUEST_INSPECTOR_MODE = false
        }
    }
}