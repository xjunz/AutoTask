package top.xjunz.tasker.task.inspector.overlay

import android.view.WindowManager
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import top.xjunz.tasker.databinding.OverlayTrashBinBinding
import top.xjunz.tasker.ktx.doWhenEnd
import top.xjunz.tasker.task.inspector.FloatingInspector

/**
 * @author xjunz 2022/10/16
 */
class TrashBinOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayTrashBinBinding>(inspector) {

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        base.width = WindowManager.LayoutParams.MATCH_PARENT
    }

    fun isInside(overlay: FloatingInspectorOverlay<*>): Boolean {
        return overlay.layoutParams.y - overlay.rootView.height / 2 >= layoutParams.y - rootView.height / 2
    }

    fun layout() {
        rootView.doOnPreDraw {
            layoutParams.y = vm.windowHeight / 2 - it.height / 2
            updateViewLayout()
        }
    }

    fun fadeIn() {
        rootView.isVisible = true
        rootView.animate().alpha(1F).start()
    }

    fun fadeOut() {
        rootView.animate().alpha(0F).doWhenEnd {
            rootView.isVisible = false
        }.start()
    }

}