/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemTaskAssistantBinding
import top.xjunz.tasker.databinding.OverlayTaskAssistantBinding
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.service.serviceController
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.task.runtime.ITaskCompletionCallback
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.task.storage.TaskStorage
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.main.EventCenter
import top.xjunz.tasker.ui.main.EventCenter.doOnEventReceived
import top.xjunz.tasker.ui.task.showcase.OneshotTaskFragment
import top.xjunz.tasker.ui.widget.FloatingDraggableLayout
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2023/02/22
 */
@SuppressLint("SetTextI18n")
class TaskAssistantOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayTaskAssistantBinding>(inspector) {

    private val tasks = mutableListOf<XTask>()

    private fun initTasks() {
        tasks.clear()
        tasks.addAll(TaskStorage.getOneshotTasks())
    }

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        layoutParams.x = vm.bubbleX
        layoutParams.y = vm.bubbleY
    }

    private val adapter by lazy {
        inlineAdapter(tasks, ItemTaskAssistantBinding::class.java, {
            binding.ibRun.setNoDoubleClickListener {
                val task = tasks[adapterPosition]
                if (task !== vm.currentRunningOneshotTask) {
                    vm.isCollapsed.value = true
                    vm.requestLaunchOneshotTask.value = tasks[adapterPosition]
                } else {
                    currentService.stopOneshotTask(task)
                }
            }
        }) { binding, pos, task ->
            if (task === vm.currentRunningOneshotTask) {
                binding.ibRun.setIconResource(R.drawable.ic_baseline_stop_24)
            } else {
                binding.ibRun.setIconResource(R.drawable.ic_baseline_play_arrow_24)
            }
            binding.tvTaskName.text = (pos + 1).toString().bold()
                .foreColored(R.color.md_theme_dark_primary.color) + "  " + task.title
        }
    }

    private val taskCompletionCallback by lazy {
        object : ITaskCompletionCallback.Stub() {
            override fun onTaskCompleted(isSuccessful: Boolean) {
                val runningTask = vm.currentRunningOneshotTask
                vm.currentRunningOneshotTask = null
                vm.makeToast(
                    R.string.format_task_finished.format(
                        if (isSuccessful) R.string.succeeded.str else R.string.failed.str
                    ), true
                )
                binding.root.post {
                    adapter.notifyItemChanged(tasks.indexOf(runningTask))
                }
            }
        }
    }

    override fun onOverlayInflated() {
        super.onOverlayInflated()
        initTasks()
        binding.rvTask.adapter = adapter
        binding.ibCollapse.setNoDoubleClickListener {
            vm.isCollapsed.value = true
        }
        (binding.root as FloatingDraggableLayout).setOnDragListener { _, offsetX, offsetY ->
            offsetViewInWindow(offsetX.toInt(), offsetY.toInt())
            vm.bubbleX = layoutParams.x
            vm.bubbleY = layoutParams.y
        }
        binding.ibCollapse.doOnPreDraw {
            binding.ibRoute.translationX = -it.width.toFloat() - 8.dp
        }
        binding.ibRoute.setNoDoubleClickListener {
            EventCenter.launchHost()
        }
        observeLivedata()
    }

    private fun observeLivedata() {
        inspector.observeMultiple(vm.currentMode, vm.isCollapsed) {
            rootView.isVisible = vm.currentMode eq InspectorMode.TASK_ASSISTANT
                    && vm.isCollapsed.isNotTrue
        }
        inspector.observe(vm.isCollapsed) {
            if (!it) {
                layoutParams.x = vm.bubbleX
                layoutParams.y = vm.bubbleY
                if (rootView.isAttachedToWindow) {
                    windowManager.updateViewLayout(rootView, layoutParams)
                }
            }
        }
        inspector.observeTransient(vm.requestLaunchOneshotTask) {
            if (serviceController.isServiceRunning) {
                rootView.post {
                    // If not present in task manager, add it first
                    LocalTaskManager.addOneshotTaskIfAbsent(it)
                    vm.currentRunningOneshotTask = it
                    currentService.scheduleOneshotTask(it, taskCompletionCallback)
                    adapter.notifyItemChanged(tasks.indexOf(it))
                }
                vm.makeToast(R.string.format_launch_oneshot_task.format(it.title))
            } else {
                EventCenter.launchHost()
                vm.makeToast(R.string.service_not_started)
            }
        }
        inspector.doOnEventReceived<Int>(OneshotTaskFragment.EVENT_ONESHOT_TASK_ADDED) {
            initTasks()
            adapter.notifyItemInserted(it)
        }
    }
}