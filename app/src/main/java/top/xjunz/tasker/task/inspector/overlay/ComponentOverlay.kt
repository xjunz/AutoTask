package top.xjunz.tasker.task.inspector.overlay

import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import top.xjunz.tasker.databinding.OverlayComponentBinding
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.ui.widget.FloatingDraggableLayout

/**
 * @author xjunz 2022/10/18
 */
class ComponentOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayComponentBinding>(inspector) {

    override fun onOverlayInflated() {
        super.onOverlayInflated()
        binding.draggableRoot.onDragListener = { state: Int, offsetX: Float, offsetY: Float ->
            if (state == FloatingDraggableLayout.STATE_DRAGGING) {
                offsetViewInWindow(offsetX.toInt(), offsetY.toInt())
            }
        }
        rootView.doOnPreDraw {
            layoutParams.y = it.height / 2 - vm.windowHeight / 2
            updateViewLayout()
        }
        inspector.observe(vm.currentComp) {
            binding.tvTitle.text = it.actLabel
            if (it.actLabel != null) {
                binding.tvTitle.append("\n")
            }
            binding.tvTitle.append(it.pkgName)
            if (it.actName != null) {
                binding.tvTitle.append("\n")
                binding.tvTitle.append(it.actName)
            }
        }
        inspector.observe(vm.currentMode) {
            rootView.isVisible = it == FloatingInspector.MODE_COMPONENT
        }
    }
}