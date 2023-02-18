/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ui.model.ActivityInfoWrapper
import top.xjunz.tasker.ui.model.PackageInfoWrapper

/**
 * @author xjunz 2022/10/08
 */
class ActivitySelectorFragment : BaseComponentFragment() {

    private val activities: MutableList<ActivityInfoWrapper> = mutableListOf()

    private lateinit var adapter: ActivityInfoAdapter

    override val index: Int = 1

    override fun findItem(item: Any): Int {
        if (item is ComponentName) {
            return activities.indexOfFirst {
                it.componentName == item
            }
        }
        return -1
    }

    private var loadingJob: Job? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(viewModel.selectedPackage) {
            loadingJob?.cancel()
            loadingJob = null
            loadActivities(it)
        }
        binding.touchEater.setOnTouchListener { _, _ -> true }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadActivities(info: PackageInfoWrapper) {
        loadingJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.progress.show()
            binding.touchEater.isVisible = true
            activities.clear()
            with(Dispatchers.IO) {
                PackageManagerBridge.loadPackageInfo(
                    info.packageName, PackageManager.GET_ACTIVITIES
                )?.activities?.filter {
                    it.exported
                }?.mapTo(activities) { activityInfo ->
                    ensureActive()
                    ActivityInfoWrapper(activityInfo, info.entranceName)
                }?.sort()
            }
            if (binding.rvList.adapter == null) {
                adapter = ActivityInfoAdapter(viewModel, activities, parentFragment)
                binding.rvList.adapter = adapter
            } else {
                adapter.notifyDataSetChanged()
                binding.rvList.scrollToPosition(0)
            }
            binding.progress.hide()
            binding.touchEater.isVisible = false
        }
    }

}