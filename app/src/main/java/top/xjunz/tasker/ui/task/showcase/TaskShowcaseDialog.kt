/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.shape.MaterialShapeDrawable
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskShowcaseBinding
import top.xjunz.tasker.engine.applet.util.hierarchy
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.task.runtime.LocalTaskManager.isEnabled
import top.xjunz.tasker.task.storage.TaskStorage
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.main.ColorScheme
import top.xjunz.tasker.ui.service.ServiceStarterDialog
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.ClickListenerUtil.setOnDoubleClickListener

/**
 * @author xjunz 2022/07/30
 */
class TaskShowcaseDialog : BaseDialogFragment<DialogTaskShowcaseBinding>() {

    override val isFullScreen = true

    override val windowAnimationStyle: Int = R.style.DialogAnimationSlide

    private val viewModel by viewModels<TaskShowcaseViewModel>()

    private val fragments = arrayOfNulls<BaseTaskShowcaseFragment>(3)

    private val viewPagerAdapter by lazy {
        object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                val f = when (position) {
                    0 -> EnabledTaskFragment()
                    1 -> ResidentTaskFragment()
                    2 -> OneshotTaskFragment()
                    else -> illegalArgument()
                }
                fragments[position] = f
                return f
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.setOnDoubleClickListener {
            Preferences.showDragToMoveTip = true
            Preferences.showToggleRelationTip = true
            Preferences.showSwipeToRemoveTip = true
            Preferences.showLongClickToSelectTip = true
        }
        binding.appBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
            v.doOnPreDraw {
                viewModel.appbarHeight.value = it.height
            }
        }
        binding.bottomBar.applySystemInsets { v, insets ->
            v.updatePadding(bottom = insets.bottom)
        }
        binding.fabAction.doOnPreDraw {
            viewModel.paddingBottom.value = it.height + binding.bottomBar.height
        }
        binding.bottomBar.background.let {
            it as MaterialShapeDrawable
            it.elevation = 4.dpFloat
            it.alpha = (.88 * 0xFF).toInt()
        }
        binding.btnServiceControl.setNoDoubleClickListener {
            if (it.isActivated) {
                mainViewModel.stopServiceConfirmation.value = true
            } else {
                ServiceStarterDialog().show(childFragmentManager)
            }
        }
        binding.fabAction.setNoDoubleClickListener {
            TaskCreatorDialog().show(childFragmentManager)
        }
        binding.viewPager.adapter = viewPagerAdapter
        binding.bottomBar.setOnItemSelectedListener {
            binding.viewPager.currentItem = binding.bottomBar.menu.indexOf(it)
            return@setOnItemSelectedListener true
        }
        binding.ibDismiss.setNoDoubleClickListener {
            dismiss()
        }
        val fabBehaviour =
            ((binding.fabAction.layoutParams as CoordinatorLayout.LayoutParams).behavior
                    as HideBottomViewOnScrollBehavior<View>)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomBar.selectedItemId = binding.bottomBar.menu.getItem(position).itemId
                ((binding.bottomBar.layoutParams as CoordinatorLayout.LayoutParams).behavior
                        as HideBottomViewOnScrollBehavior<View>).slideUp(binding.bottomBar, true)
                fabBehaviour.slideUp(binding.fabAction, true)
                binding.appBar.setLiftOnScrollTargetView(fragments[position]?.getScrollTarget())
            }
        })
        observeTransient(viewModel.requestEditTask) {
            val task = it.first
            val prevChecksum = task.checksum
            FlowEditorDialog().initBase(task, false).doOnTaskEdited {
                viewModel.updateTask(prevChecksum, task)
            }.setFlowToNavigate(it.second?.hierarchy).show(childFragmentManager)
        }
        observeTransient(viewModel.requestTrackTask) {
            FlowEditorDialog().initBase(it, true).setTrackMode().show(childFragmentManager)
        }
        observeTransient(viewModel.requestToggleTask) { task ->
            when (task.metadata.taskType) {
                XTask.TYPE_RESIDENT -> {
                    val title = if (task.isEnabled) R.string.prompt_disable_task.text
                    else R.string.prompt_enable_task.text
                    requireContext().makeSimplePromptDialog(msg = title) {
                        viewModel.toggleTask(task)
                    }.setNeutralButton(
                        if (task.isEnabled) R.string.disable_all_tasks
                        else R.string.enable_all_tasks
                    ) { _, _ ->
                        TaskStorage.getAllTasks().filter {
                            it.metadata.taskType == XTask.TYPE_RESIDENT && it.isEnabled == task.isEnabled
                        }.forEach {
                            viewModel.toggleTask(it)
                        }
                    }.show().also {
                        it.getButton(DialogInterface.BUTTON_NEUTRAL)
                            .setTextColor(ColorScheme.colorError)
                    }
                }
                XTask.TYPE_ONESHOT -> {
                    FloatingInspectorDialog().setMode(InspectorMode.TASK_ASSISTANT).doOnSucceeded {
                        floatingInspector.viewModel.isCollapsed.value = false
                    }.show(childFragmentManager)
                }
            }
        }
        observeDangerousConfirmation(
            viewModel.requestDeleteTask,
            R.string.prompt_delete_task,
            R.string.delete
        ) {
            viewModel.deleteRequestedTask()
        }
        observeTransient(viewModel.onNewTaskAdded) {
            when (it.metadata.taskType) {
                XTask.TYPE_ONESHOT -> binding.viewPager.currentItem = 2
                XTask.TYPE_RESIDENT -> binding.viewPager.currentItem = 1
            }
        }
        observeTransient(viewModel.requestAddNewTask) {
            viewModel.addRequestedTask()
        }
        observeTransient(viewModel.onTaskDeleted) {
            fabBehaviour.slideUp(binding.fabAction)
        }
        observe(mainViewModel.isServiceRunning) {
            binding.btnServiceControl.isActivated = it
            if (it) {
                binding.btnServiceControl.setText(R.string.stop_service)
                binding.btnServiceControl.setIconResource(R.drawable.ic_baseline_stop_24)
            } else {
                binding.btnServiceControl.setText(R.string.start_service)
                binding.btnServiceControl.setIconResource(R.drawable.ic_baseline_play_arrow_24)
            }
        }
    }
}