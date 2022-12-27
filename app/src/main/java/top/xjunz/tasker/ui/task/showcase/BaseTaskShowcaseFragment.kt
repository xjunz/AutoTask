/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.transition.platform.MaterialFadeThrough
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.FragmentTaskShowcaseBinding
import top.xjunz.tasker.databinding.ItemTaskShowcaseBinding
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.ui.base.BaseFragment
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/12/16
 */
abstract class BaseTaskShowcaseFragment : BaseFragment<FragmentTaskShowcaseBinding>() {

    override val bindingRequiredSuperClassDepth: Int = 2

    protected val viewModel by lazy {
        requireParentFragment().viewModels<TaskShowcaseViewModel>().value
    }

    protected val taskList = mutableListOf<XTask>()

    protected val adapter = TaskAdapter()

    abstract fun initTaskList(): List<XTask>

    @SuppressLint("ClickableViewAccessibility")
    protected inner class TaskViewHolder(val binding: ItemTaskShowcaseBinding) :
        ViewHolder(binding.root) {
        init {
            binding.msEnabled.setOnInteractiveCheckedChangedListener { v, isChecked ->
                // Do not toggle the Switch instantly, because we want a confirmation.
                v.isChecked = !isChecked
                viewModel.requestToggleTask.value = taskList[adapterPosition]
            }
            binding.ibDelete.setAntiMoneyClickListener {
                viewModel.requestDeleteTask.value = taskList[adapterPosition]
            }
            binding.ibEdit.setAntiMoneyClickListener {
                val task = taskList[adapterPosition]
                val prevChecksum = task.checksum
                FlowEditorDialog().init(task).doOnTaskEdited {
                    viewModel.updateTask(prevChecksum, task)
                }.show(childFragmentManager)
            }
        }
    }

    protected inner class TaskAdapter : RecyclerView.Adapter<TaskViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            return TaskViewHolder(
                ItemTaskShowcaseBinding.inflate(
                    requireActivity().layoutInflater, parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task = taskList[position]
            val metadata = task.metadata
            val b = holder.binding
            b.msEnabled.isChecked = task.isEnabled
            b.tvTaskName.text = metadata.title
            b.tvTaskDesc.text = metadata.description
            b.tvAuthor.text = metadata.author
            if (metadata.description.isNullOrEmpty()) {
                b.tvTaskDesc.text = R.string.no_desc_provided.text
                b.tvTaskDesc.isEnabled = false
            } else {
                b.tvTaskDesc.text = metadata.description
                b.tvTaskDesc.isEnabled = true
            }
            b.tvBadge.isVisible = task.isPreload
            if (task.isEnabled) {
                b.msEnabled.setText(R.string.is_enabled)
            } else {
                b.msEnabled.setText(R.string.not_is_enabled)
            }
            b.ibDelete.isVisible = !task.isEnabled
            b.ibEdit.isVisible = !task.isEnabled
            b.ibTrack.isVisible = task.isEnabled
        }

        override fun getItemCount() = taskList.size

    }

    protected fun togglePlaceholder(visible: Boolean) {
        binding.root.beginAutoTransition(MaterialFadeThrough())
        binding.groupPlaceholder.isVisible = visible
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(viewModel.appbarHeight) {
            binding.rvTaskList.updatePadding(top = it)
        }
        observe(viewModel.bottomBarHeight) {
            binding.rvTaskList.updatePadding(bottom = it)
        }
        observe(peekMainViewModel().allTaskLoaded) {
            taskList.clear()
            taskList.addAll(initTaskList())
            binding.rvTaskList.adapter = adapter
            if (taskList.isEmpty()) {
                togglePlaceholder(true)
            }
        }
        observeTransient(viewModel.onTaskDeleted) {
            val index = taskList.indexOf(it)
            if (index > -1) {
                taskList.removeAt(index)
                adapter.notifyItemRemoved(index)
                if (taskList.isEmpty()) togglePlaceholder(true)
            }
        }
        observeTransient(viewModel.onTaskToggled) {
            adapter.notifyItemChanged(taskList.indexOf(it), true)
        }
        observeTransient(viewModel.onTaskUpdated) {
            adapter.notifyItemChanged(taskList.indexOf(it), true)
        }
    }
}