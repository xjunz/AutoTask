package top.xjunz.tasker.ui.task.editor

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import top.xjunz.tasker.databinding.FragmentAppletPanelBinding
import top.xjunz.tasker.ui.base.BaseFragment
import top.xjunz.tasker.ui.task.TaskEditorViewModel

/**
 * @author xjunz 2022/09/26
 */
class AppletOperationPanelFragment : BaseFragment<FragmentAppletPanelBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.doOnPreDraw {
            requireParentFragment().viewModels<TaskEditorViewModel>().value.panelHeights[0] =
            view.height
        }
    }
}