package top.xjunz.tasker.ui.task

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskEditorBinding
import top.xjunz.tasker.engine.flow.Flow
import top.xjunz.tasker.ktx.applySystemInsets
import top.xjunz.tasker.ktx.doOnCreated
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ui.base.BaseDialogFragment

/**
 * @author xjunz 2022/08/22
 */
class TaskEditorDialog : BaseDialogFragment<DialogTaskEditorBinding>() {

    private val viewModel by viewModels<TaskEditorViewModel>()

    private val adapter by lazy { TaskFlowAdapter(this, viewModel) }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        observeLiveData()
    }

    @SuppressLint("RestrictedApi", "VisibleForTests")
    private fun initViews() {
        binding.appBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.appBar.doOnPreDraw {
            binding.rvTaskEditor.updatePadding(top = it.height)
        }
        binding.rvTaskEditor.adapter = adapter
        adapter.setFlow(viewModel.flow)
        if (viewModel.isNewTask) {
            binding.tvTitle.text = R.string.create_task.text
        } else {
            binding.tvTitle.text = R.string.edit_task.text
        }
        bottomSheetBehavior =
            (binding.scrollView.layoutParams as CoordinatorLayout.LayoutParams).behavior as BottomSheetBehavior<*>
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) adapter.clearSelection()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        bottomSheetBehavior.disableShapeAnimations()
        binding.scrollView.applySystemInsets { v, insets ->
            v.updatePadding(bottom = insets.bottom)
        }
    }

    override fun onBackPressed(): Boolean {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            return true
        }
        return super.onBackPressed()
    }

    private fun observeLiveData() {
        observe(viewModel.selectedIndex) {
            if (it == -1) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        observe(viewModel.selectedItem) {
            binding.btnInvert.isEnabled = it.isInvertible
            binding.btnDelete.isEnabled = !it.isRequired
            binding.btnReplace.isEnabled = !it.isRequired
        }
    }

    fun setFlow(flow: Flow): TaskEditorDialog {
        doOnCreated {
            viewModel.isNewTask = false
            viewModel.flow = flow
        }
        return this
    }

}