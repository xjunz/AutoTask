/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogComponentSelectorBinding
import top.xjunz.tasker.databinding.ItemActivityInfoBinding
import top.xjunz.tasker.databinding.ItemApplicationInfoBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.main.EventCenter.doOnEventRouted
import top.xjunz.tasker.ui.model.ActivityInfoWrapper
import top.xjunz.tasker.ui.model.PackageInfoWrapper
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.ui.task.selector.ShoppingCartIntegration
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/10/07
 */
class ComponentSelectorDialog : BaseDialogFragment<DialogComponentSelectorBinding>() {

    companion object {
        const val MODE_PACKAGE = 1
        const val MODE_ACTIVITY = 2
    }

    override val isFullScreen: Boolean = true

    private val viewModel by viewModels<ComponentSelectorViewModel>()

    val appBar get() = binding.appBar

    fun setMode(mode: Int): ComponentSelectorDialog = doWhenCreated {
        viewModel.mode = mode
    }

    fun setSelectedPackages(selected: Collection<String>) = doWhenCreated {
        viewModel.selectedPackages.addAll(selected)
        viewModel.selectedCount.value = selected.size
    }

    fun doOnCompleted(block: (Collection<String>) -> Unit) = doWhenCreated {
        viewModel.onCompleted = block
    }

    fun setSingleSelection(single: Boolean) = doWhenCreated {
        viewModel.isSingleSelection = single
    }

    fun setSelectedActivities(selected: Collection<String>) = doWhenCreated {
        viewModel.selectedActivities.addAll(selected.map {
            ComponentName.unflattenFromString(it)!!
        })
        viewModel.selectedCount.value = selected.size
    }

    fun setTitle(title: CharSequence?) = doWhenCreated {
        viewModel.title = title
    }

    private val bottomPkgAdapter by lazy {
        inlineAdapter(
            viewModel.selectedPackages, ItemApplicationInfoBinding::class.java,
            {
                binding.root.background = R.drawable.bg_selectable_surface.getDrawable()
                binding.root.updateLayoutParams<MarginLayoutParams> {
                    updateMargins(0, 0, 0, 4.dp)
                }
                binding.root.setNoDoubleClickListener {
                    viewModel.itemToRemove.value =
                        viewModel.selectedPackages.reversed()[adapterPosition]
                }
            }) { b, p, _ ->
            val pkgName = viewModel.selectedPackages[viewModel.selectedPackages.lastIndex - p]
            val info = viewModel.findPackageInfo(pkgName)
            b.tvExtraInfo.isVisible = false
            b.tvPkgName.text = pkgName
            b.tvApplicationName.text = info?.label ?: pkgName
            viewModel.iconLoader.loadIconTo(info, b.ivIcon, this)
        }
    }

    private val bottomActAdapter by lazy {
        inlineAdapter(
            viewModel.selectedActivities, ItemActivityInfoBinding::class.java,
            {
                binding.root.background = R.drawable.bg_selectable_surface.getDrawable()
                binding.root.updateLayoutParams<MarginLayoutParams> {
                    updateMargins(0, 0, 0, 4.dp)
                }
                binding.tvActivityName.isVisible = false
                binding.tvBadge.isVisible = false
                binding.root.setNoDoubleClickListener {
                    viewModel.itemToRemove.value =
                        viewModel.selectedActivities.reversed()[adapterPosition]
                }
            }) { b, p, _ ->
            val comp = viewModel.selectedActivities[viewModel.selectedActivities.lastIndex - p]
            val info = viewModel.findPackageInfo(comp.packageName)
            viewModel.iconLoader.loadIconTo(info, b.ivIcon, this)
            b.tvFullName.text = comp.flattenToShortString()
        }
    }

    private val shoppingCartIntegration by lazy {
        ShoppingCartIntegration(binding.shoppingCart, viewModel, binding.viewPager)
    }

    private val popupMenu by lazy {
        PopupMenu(requireContext(), binding.ibSortBy).also { popup ->
            popup.inflate(R.menu.package_filter)
            popup.setOnMenuItemClickListener l@{
                when (it.itemId) {
                    R.id.item_sort_by -> {
                        popup.menu.findItem(
                            when (viewModel.sortBy) {
                                PackageInfoWrapper.SORT_BY_LABEL -> R.id.item_sort_by_name
                                PackageInfoWrapper.SORT_BY_SUSPICION -> R.id.item_sort_by_sus
                                PackageInfoWrapper.SORT_BY_SIZE -> R.id.item_sort_by_size
                                PackageInfoWrapper.SORT_BY_FIRST_INSTALL_TIME -> R.id.item_sort_by_install_time
                                else -> illegalArgument("sortBy", viewModel.sortBy)
                            }
                        ).isChecked = true
                    }
                    R.id.item_sort_by_name -> viewModel.sortPackagesBy(PackageInfoWrapper.SORT_BY_LABEL)
                    R.id.item_sort_by_sus -> viewModel.sortPackagesBy(PackageInfoWrapper.SORT_BY_SUSPICION)
                    R.id.item_sort_by_size -> viewModel.sortPackagesBy(PackageInfoWrapper.SORT_BY_SIZE)
                    R.id.item_sort_by_install_time -> viewModel.sortPackagesBy(PackageInfoWrapper.SORT_BY_FIRST_INSTALL_TIME)
                    R.id.item_reverse_order -> viewModel.reverseOrder(!it.isChecked)
                    R.id.item_show_system_app -> viewModel.filterSystemApps(!it.isChecked)
                    else -> return@l false
                }
                return@l true
            }
        }
    }

    private val viewPagerAdapter by lazy {
        object : FragmentStateAdapter(this) {

            override fun getItemCount(): Int {
                return if (viewModel.mode == MODE_PACKAGE) 1 else 2
            }

            override fun createFragment(position: Int): Fragment {
                return if (position == 0) PackageSelectorFragment() else ActivitySelectorFragment()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.sortPackagesBy(PackageInfoWrapper.SORT_BY_SUSPICION)
        binding.tvTitle.text = viewModel.title
        binding.appBar.oneShotApplySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.appBar.doOnPreDraw {
            viewModel.appBarHeight.value = it.height
        }
        binding.ibSortBy.setOnTouchListener(popupMenu.dragToOpenListener)
        binding.ibSortBy.setNoDoubleClickListener {
            popupMenu.show()
            popupMenu.menu.findItem(R.id.item_reverse_order).isChecked = viewModel.isOrderReversed
            popupMenu.menu.findItem(R.id.item_show_system_app).isChecked = viewModel.showSystemApps
        }
        shoppingCartIntegration.init(this)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.doOnItemSelected {
            viewModel.currentItem.value = it
        }
        binding.shoppingCart.btnCount.setNoDoubleClickListener {
            if (viewModel.selectedCount notEq 0)
                viewModel.showClearAllDialog.value = true
        }
        binding.shoppingCart.circularRevealContainer.doOnPreDraw {
            binding.fabInspector.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = it.height + 48.dp
            }
        }
        binding.shoppingCart.btnComplete.setNoDoubleClickListener {
            if (viewModel.complete()) {
                dismiss()
            } else {
                toast(R.string.nothing_selected)
            }
        }
        binding.fabInspector.setNoDoubleClickListener {
            FloatingInspectorDialog().setMode(InspectorMode.COMPONENT).show(childFragmentManager)
        }
        observeLiveData()
    }

    private fun selectPackage(pkgName: String): Boolean {
        if (viewModel.selectedPackages.contains(pkgName)) {
            viewModel.itemToRemove.value = pkgName
            return false
        } else {
            if (viewModel.isSingleSelection) {
                val removed = viewModel.selectedPackages.singleOrNull()
                if (removed != null) {
                    viewModel.itemToRemove.value = removed
                }
            }
            viewModel.selectedPackages.add(pkgName)
            viewModel.addedItem.value = pkgName
            return true
        }
    }

    private fun selectActivity(compName: ComponentName): Boolean {
        if (viewModel.selectedActivities.contains(compName)) {
            viewModel.itemToRemove.value = compName
            return false
        } else {
            if (viewModel.isSingleSelection) {
                val removed = viewModel.selectedActivities.singleOrNull()
                if (removed != null) {
                    viewModel.itemToRemove.value = removed
                }
            }
            viewModel.selectedActivities.add(compName)
            viewModel.addedItem.value = compName
            return true
        }
    }

    fun onPackageItemClicked(info: PackageInfoWrapper, binding: ItemApplicationInfoBinding) {
        if (viewModel.mode == MODE_PACKAGE) {
            if (selectPackage(info.packageName)) {
                shoppingCartIntegration.animateIntoShopCart(binding.ivIcon, false)
            }
        } else {
            viewModel.selectedPackage.value = info
            this@ComponentSelectorDialog.binding.viewPager.currentItem = 1
        }
    }

    fun onActivityItemClicked(info: ActivityInfoWrapper, binding: ItemActivityInfoBinding) {
        if (selectActivity(info.componentName)) {
            shoppingCartIntegration.animateIntoShopCart(binding.ivIcon, false)
        }
    }

    private fun observeLiveData() {
        observe(viewModel.selectedCount) {
            binding.shoppingCart.btnCount.text = if (viewModel.mode == MODE_PACKAGE) {
                R.string.format_clear_selected_pkg.format(it)
            } else {
                R.string.format_clear_selected_act.format(it)
            }
        }
        observeTransient(viewModel.addedItem) {
            if (viewModel.mode == MODE_PACKAGE) {
                val index = viewModel.selectedPackages.indexOf(it)
                bottomPkgAdapter.notifyItemInserted(viewModel.selectedPackages.lastIndex - index)
            } else {
                val index = viewModel.selectedActivities.indexOf(it)
                bottomActAdapter.notifyItemInserted(viewModel.selectedActivities.lastIndex - index)
            }
            binding.shoppingCart.rvBottom.scrollToPosition(0)
            viewModel.selectedCount.inc()
        }
        observeTransient(viewModel.itemToRemove) {
            if (viewModel.mode == MODE_PACKAGE) {
                val index = viewModel.selectedPackages.indexOf(it)
                viewModel.selectedPackages.removeAt(index)
                bottomPkgAdapter.notifyItemRemoved(viewModel.selectedPackages.size - index)
            } else {
                val index = viewModel.selectedActivities.indexOf(it)
                viewModel.selectedActivities.removeAt(index)
                bottomActAdapter.notifyItemRemoved(viewModel.selectedActivities.size - index)
            }
            viewModel.selectedCount.dec()
        }
        observeConfirmation(viewModel.showClearAllDialog, R.string.prompt_clear_all) {
            if (viewModel.mode == MODE_ACTIVITY) {
                val prevSize = viewModel.selectedActivities.size
                viewModel.selectedActivities.clear()
                bottomActAdapter.notifyItemRangeRemoved(0, prevSize)
            } else {
                val prevSize = viewModel.selectedPackages.size
                viewModel.selectedPackages.clear()
                bottomPkgAdapter.notifyItemRangeRemoved(0, prevSize)
            }
            viewModel.selectedCount.value = 0
            shoppingCartIntegration.collapse()
            viewModel.onSelectionCleared.value = true
        }
        observeOnce(viewModel.currentPackages) {
            if (viewModel.mode == MODE_ACTIVITY)
                binding.shoppingCart.rvBottom.adapter = bottomActAdapter
            else
                binding.shoppingCart.rvBottom.adapter = bottomPkgAdapter
        }
        doOnEventRouted<ComponentInfoWrapper>(FloatingInspector.EVENT_COMPONENT_SELECTED) {
            if (viewModel.mode == MODE_PACKAGE) {
                if (viewModel.selectedPackages.contains(it.packageName)) {
                    toast(R.string.format_already_existed.format(it.packageName))
                } else {
                    toast(R.string.format_added.format(it.packageName))
                    selectPackage(it.packageName)
                }
            } else {
                it.getComponentName()?.run {
                    if (viewModel.selectedActivities.contains(this)) {
                        toast(R.string.format_already_existed.format(it.activityName))
                    } else {
                        toast(R.string.format_added.format(it.activityName))
                        selectActivity(this)
                    }
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (viewModel.currentItem eq 1) {
            binding.viewPager.currentItem = 0
            return true
        }
        return super.onBackPressed()
    }
}