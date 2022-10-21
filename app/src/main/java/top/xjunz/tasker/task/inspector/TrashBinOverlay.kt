package top.xjunz.tasker.task.inspector

import android.view.WindowManager
import androidx.core.view.isVisible
import top.xjunz.tasker.databinding.OverlayTrashBinBinding
import top.xjunz.tasker.ktx.doWhenEnd

/**
 * @author xjunz 2022/10/16
 */
class TrashBinOverlay(inspector: FloatingInspector) :
    BaseOverlay<OverlayTrashBinBinding>(inspector) {

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        base.width = WindowManager.LayoutParams.MATCH_PARENT
    }

    fun isInside(overlay: BaseOverlay<*>): Boolean {
        return overlay.layoutParams.y - overlay.rootView.height / 2 >= layoutParams.y - rootView.height / 2
    }

    fun layout() {
        layoutParams.y = vm.windowHeight / 2 - rootView.height / 2
        updateViewLayout()
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