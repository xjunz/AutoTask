package top.xjunz.tasker.task.inspector.overlay

import android.view.WindowManager
import androidx.core.view.doOnPreDraw
import top.xjunz.tasker.databinding.OverlayWindowBoundsDetectorBinding
import top.xjunz.tasker.task.inspector.FloatingInspector

/**
 * @author xjunz 2022/10/31
 */
class BoundsDetectorOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayWindowBoundsDetectorBinding>(inspector) {

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        base.alpha = 0F
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.height = WindowManager.LayoutParams.MATCH_PARENT
    }

    override fun onOverlayInflated() {
        super.onOverlayInflated()
        rootView.doOnPreDraw {
            vm.windowHeight = it.height
            vm.windowWidth = it.width
        }
    }
}