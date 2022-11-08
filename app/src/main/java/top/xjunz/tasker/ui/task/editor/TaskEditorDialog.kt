package top.xjunz.tasker.ui.task.editor

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskEditorBinding
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.base.BaseDialogFragment

/**
 * @author xjunz 2022/08/22
 */
class TaskEditorDialog : BaseDialogFragment<DialogTaskEditorBinding>() {

    private val viewModel by viewModels<TaskEditorViewModel>()

    private val adapter by lazy {
        TaskFlowAdapter(this, viewModel)
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(savedInstanceState)
        observeLiveData()
    }

    private fun initViews(savedInstanceState: Bundle?) {
        binding.appBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.rvTaskEditor.adapter = adapter
        if (viewModel.isNewTask) {
            binding.tvTitle.text = R.string.create_task.text
            if (savedInstanceState == null)
                viewModel.generateDefaultFlow()
        } else {
            binding.tvTitle.text = R.string.edit_task.text
        }
        bottomSheetBehavior = binding.scrollView.disableBottomSheetShapeAnimation()
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) viewModel.singleSelect(-1)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
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
        observeNostalgic(viewModel.singleSelectionIndex) { prev, cur ->
            if (prev != null && prev != -1)
                adapter.notifyItemChanged(prev, true)
            adapter.notifyItemChanged(cur, true)
        }
        observe(viewModel.applets) {
            adapter.submitList(it)
        }
        observeTransient(viewModel.changedApplet) {
            adapter.notifyItemChanged(viewModel.applets.require().indexOf(it), true)
        }
    }

    fun setFlow(flow: Flow) = doWhenCreated {
        viewModel.isNewTask = false
        viewModel.setFlow(flow)
    }

}