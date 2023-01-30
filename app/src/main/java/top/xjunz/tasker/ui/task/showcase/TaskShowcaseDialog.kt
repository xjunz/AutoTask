/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.shape.MaterialShapeDrawable
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskShowcaseBinding
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.hierarchy
import top.xjunz.tasker.task.runtime.LocalTaskManager.isEnabled
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.ui.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.service.ServiceStarterDialog
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/07/30
 */
class TaskShowcaseDialog : BaseDialogFragment<DialogTaskShowcaseBinding>() {

    override val isFullScreen = true

    private val viewModel by viewModels<TaskShowcaseViewModel>()

    private val fragments = arrayOfNulls<BaseTaskShowcaseFragment>(3)

    private val viewPagerAdapter by lazy {
        object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                val f = when (position) {
                    0 -> EnabledTaskFragment()
                    1 -> ResidentTaskFragment()
                    2 -> EnabledTaskFragment()
                    else -> illegalArgument()
                }
                fragments[position] = f
                return f
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        val mvm = peekMainViewModel()
        binding.btnServiceControl.setAntiMoneyClickListener {
            if (it.isActivated) {
                mvm.stopServiceConfirmation.value = true
            } else {
                ServiceStarterDialog().show(childFragmentManager)
            }
        }
        binding.fabAction.setAntiMoneyClickListener {
            TaskCreatorDialog().show(childFragmentManager)
        }
        binding.viewPager.adapter = viewPagerAdapter
        binding.bottomBar.setOnItemSelectedListener {
            binding.viewPager.currentItem = binding.bottomBar.menu.indexOf(it)
            return@setOnItemSelectedListener true
        }
        binding.ibDismiss.setAntiMoneyClickListener {
            dismiss()
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomBar.selectedItemId = binding.bottomBar.menu.getItem(position).itemId
                ((binding.bottomBar.layoutParams as CoordinatorLayout.LayoutParams).behavior
                        as HideBottomViewOnScrollBehavior<View>).slideUp(binding.bottomBar, true)
                ((binding.fabAction.layoutParams as CoordinatorLayout.LayoutParams).behavior
                        as HideBottomViewOnScrollBehavior<View>).slideUp(binding.fabAction, true)
                binding.appBar.setLiftOnScrollTargetView(fragments[position]?.getScrollTarget())
            }
        })
        observeTransient(viewModel.requestEditTask) {
            val task = it.first
            val prevChecksum = task.checksum
            spyOnFragment(FlowEditorDialog().initBase(task, false).doOnTaskEdited {
                viewModel.updateTask(prevChecksum, task)
            }.setFlowToNavigate(it.second?.hierarchy).show(childFragmentManager))
        }
        observeTransient(viewModel.requestTrackTask) {
            spyOnFragment(
                FlowEditorDialog().initBase(it, true).setTrackMode().show(childFragmentManager)
            )
        }
        observeDialog(viewModel.requestToggleTask) {
            val title = if (it.isEnabled) R.string.prompt_disable_task.text
            else R.string.prompt_enable_task.text
            requireContext().makeSimplePromptDialog(msg = title) {
                viewModel.toggleRequestedTask()
            }.show()
        }
        observeDialog(viewModel.requestDeleteTask) {
            requireActivity().makeSimplePromptDialog(msg = R.string.prompt_delete_task)
                .setPositiveButton(R.string.delete.text.foreColored(ColorScheme.colorError)) { _, _ ->
                    viewModel.deleteRequestedTask()
                }.show()
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
        observe(mvm.isServiceRunning) {
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

    private fun spyOnFragment(fragment: Fragment) {
        val tag = fragment.tag
        childFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                    super.onFragmentDestroyed(fm, f)
                    if (f.tag === tag) {
                        viewModel.isPaused.value = false
                        childFragmentManager.unregisterFragmentLifecycleCallbacks(this)
                        binding.appBar.setLiftOnScrollTargetView(fragments[binding.viewPager.currentItem]?.getScrollTarget())
                    }
                }

                override fun onFragmentAttached(
                    fm: FragmentManager,
                    f: Fragment,
                    context: Context
                ) {
                    if (f.tag === tag) {
                        viewModel.isPaused.value = true
                    }
                }
            }, false
        )
    }

}