/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskShowcaseBinding
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.service.ServiceStarterDialog
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/07/30
 */
class TaskShowcaseDialog : BaseDialogFragment<DialogTaskShowcaseBinding>() {

    override val isFullScreen = true

    private val viewModel by viewModels<TaskShowcaseViewModel>()

    private val viewPagerAdapter by lazy {
        object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> EnabledTaskFragment()
                    1 -> ResidentTaskFragment()
                    2 -> EnabledTaskFragment()
                    else -> illegalArgument()
                }
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
            v.doOnPreDraw {
                binding.fabAction.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = v.height + 16.dp
                }
                viewModel.bottomBarHeight.value = it.height
            }
        }
        val mvm = peekMainViewModel()
        binding.btnServiceControl.setAntiMoneyClickListener {
            if (it.isActivated) {
                mvm.showStopConfirmation.value = true
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
        binding.appBar.addLiftOnScrollListener { _, _ ->
            viewModel.liftedStates[binding.viewPager.currentItem] = binding.appBar.isLifted
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomBar.selectedItemId = binding.bottomBar.menu.getItem(position).itemId
                ((binding.bottomBar.layoutParams as CoordinatorLayout.LayoutParams).behavior
                        as HideBottomViewOnScrollBehavior<View>).slideUp(binding.bottomBar, true)
                ((binding.fabAction.layoutParams as CoordinatorLayout.LayoutParams).behavior
                        as HideBottomViewOnScrollBehavior<View>).slideUp(binding.fabAction, true)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    if (viewModel.liftedStates[binding.viewPager.currentItem])
                        binding.appBar.isLifted = true
                }
            }
        })
        observeDialog(viewModel.requestToggleTask) {
            val title = if (it.isEnabled) R.string.prompt_disable_task.text
            else R.string.prompt_enable_task.text
            requireContext().makeSimplePromptDialog(msg = title) {
                viewModel.toggleRequestedTask()
            }.show()
        }
        observeConfirmation(viewModel.requestDeleteTask, R.string.prompt_delete_task) {
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
        observe(peekMainViewModel().isRunning) {
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