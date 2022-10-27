package top.xjunz.tasker.task.inspector.overlay

import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.platform.MaterialFadeThrough
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.xjunz.tasker.databinding.OverlayToastBinding
import top.xjunz.tasker.ktx.beginAutoTransition
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.task.inspector.FloatingInspector

/**
 * @author xjunz 2022/10/16
 */
class ToastOverlay(inspector: FloatingInspector) : FloatingInspectorOverlay<OverlayToastBinding>(inspector) {

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.height = WindowManager.LayoutParams.MATCH_PARENT
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.flags = base.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    }

    private var dismissJob: Job? = null

    override fun onOverlayInflated() {
        super.onOverlayInflated()
        inspector.observeTransient(vm.toastText) {
            if (binding.tvToast.isVisible) {
                binding.tvToast.isVisible = false
                dismissJob?.cancel()
            }
            rootView.beginAutoTransition(MaterialFadeThrough())
            binding.tvToast.text = it
            binding.tvToast.isVisible = true
            dismissJob = inspector.lifecycleScope.launch {
                delay(1500)
                binding.root.beginAutoTransition(MaterialFadeThrough())
                binding.tvToast.isVisible = false
            }
        }
    }

}