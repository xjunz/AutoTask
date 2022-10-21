package top.xjunz.tasker.ui.task

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskEditorBinding
import top.xjunz.tasker.engine.base.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog

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
        bottomSheetBehavior = binding.scrollView.disableBottomSheetShapeAnimation()
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) adapter.clearSelection()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        binding.scrollView.applySystemInsets { v, insets ->
            v.updatePadding(bottom = insets.bottom)
        }
        binding.btnAddInside.setOnClickListener {
            FlowEditorDialog().setTitle(binding.btnAddInside.text)
                .show(parentFragmentManager)
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
        observe(viewModel.selectedFlowIndex) {
            if (it == -1) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        observe(viewModel.currentPage) {
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