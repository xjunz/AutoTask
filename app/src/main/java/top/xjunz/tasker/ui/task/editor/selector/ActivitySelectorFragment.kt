package top.xjunz.tasker.ui.task.editor.selector

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
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.util.PackageInfoLoader

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
                PackageInfoLoader.loadPackageInfo(
                    info.packageName, PackageManager.GET_ACTIVITIES
                )!!.activities.mapTo(activities) {
                    ensureActive()
                    ActivityInfoWrapper(it, info.entranceName)
                }.sort()
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