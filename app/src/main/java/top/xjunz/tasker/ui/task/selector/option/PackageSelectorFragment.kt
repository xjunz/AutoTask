/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.option

import android.content.ComponentName
import android.os.Bundle
import android.view.View
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.require

/**
 * @author xjunz 2022/10/09
 */
class PackageSelectorFragment : BaseComponentFragment() {

    private lateinit var pkgInfoAdapter: PackageInfoAdapter

    override val index: Int = 0

    override fun findItem(item: Any): Int {
        if (item is String) {
            return viewModel.currentPackages.require().indexOfFirst {
                it.packageName == item
            }
        }
        return -1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(viewModel.currentPackages) {
            if (binding.rvList.adapter == null) {
                pkgInfoAdapter = PackageInfoAdapter(it, viewModel, parentFragment)
                binding.rvList.adapter = pkgInfoAdapter
            } else {
                pkgInfoAdapter.update(it)
            }
        }
        observeTransient(viewModel.addedItem) {
            if (viewModel.mode == ComponentSelectorDialog.MODE_ACTIVITY) {
                it as ComponentName
                val index = viewModel.currentPackages.require().indexOfFirst { info ->
                    info.packageName == it.packageName
                }
                if (index >= 0) {
                    viewModel.currentPackages.require()[index].selectedActCount++
                    pkgInfoAdapter.notifyItemChanged(index, true)
                }
            }
        }
        observeTransient(viewModel.removedItem) {
            if (viewModel.mode == ComponentSelectorDialog.MODE_ACTIVITY) {
                it as ComponentName
                val index = viewModel.currentPackages.require().indexOfFirst { info ->
                    info.packageName == it.packageName
                }
                if (index >= 0) {
                    viewModel.currentPackages.require()[index].selectedActCount--
                    pkgInfoAdapter.notifyItemChanged(index, true)
                }
            }
        }
    }


}