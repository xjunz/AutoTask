package top.xjunz.tasker.ui.task

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.updatePadding
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskShowcaseBinding
import top.xjunz.tasker.ktx.applySystemInsets
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.core.InspectorController
import top.xjunz.tasker.ui.base.BaseDialogFragment

/**
 * @author xjunz 2022/07/30
 */
class TaskShowcaseDialog : BaseDialogFragment<DialogTaskShowcaseBinding>() {

    private lateinit var overlaySettingLauncher: ActivityResultLauncher<Intent>

    private lateinit var accessibilitySettingsLauncher: ActivityResultLauncher<Intent>

    override val isFullScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlaySettingLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Settings.canDrawOverlays(requireActivity())) {
                    if (InspectorController.isReady()) {
                        InspectorController.showInspector()
                    } else {
                        launchAccessibilitySettings()
                    }
                } else {
                    toast(R.string.failed)
                }
            }
        accessibilitySettingsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (InspectorController.isReady()) {
                    InspectorController.showInspector()
                } else {
                    toast(R.string.failed)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.fabInspector.setOnClickListener {
            if (!InspectorController.canDrawOverlay()) {
                launchOverlaySettings()
            } else if (!InspectorController.canRetrieveWindowRoot()) {
                launchAccessibilitySettings()
            } else {
                InspectorController.showInspector()
            }
        }
        binding.ibCreateTask.setOnClickListener {
            TaskEditorDialog().show(parentFragmentManager)
        }
    }

    private fun <T> launchIntentSafely(launcher: ActivityResultLauncher<T>, param: T) =
        runCatching {
            launcher.launch(param)
        }.isSuccess

    private fun launchAccessibilitySettings() {
        launchIntentSafely(
            accessibilitySettingsLauncher, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        )
        toast(R.string.pls_start_a11y_service_manually)
    }

    private fun launchOverlaySettings() {
        launchIntentSafely(
            overlaySettingLauncher,
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).setData(Uri.parse("package:${BuildConfig.APPLICATION_ID}"))
        )
        toast(R.string.pls_enable_overlay_manually)
    }
}