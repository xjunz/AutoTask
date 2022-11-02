package top.xjunz.tasker.ui.task.editor.selector

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Bundle
import android.transition.ChangeBounds
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogComponentSelectorBinding
import top.xjunz.tasker.databinding.ItemActivityInfoBinding
import top.xjunz.tasker.databinding.ItemApplicationInfoBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.task.editor.ShoppingCartIntegration

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

    fun setSelectedActivities(selected: Collection<String>) = doWhenCreated {
        viewModel.selectedActivities.addAll(selected.map {
            ComponentName.unflattenFromString(it)!!
        })
        viewModel.selectedCount.value = selected.size
    }

    fun setTitle(title: CharSequence) = doWhenCreated {
        viewModel.title = title
    }

    private val bottomPkgAdapter by lazy {
        inlineAdapter(
            viewModel.selectedPackages, ItemApplicationInfoBinding::class.java,
            {
                binding.tvApplicationName.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                binding.root.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_option_frame)
                binding.root.updateLayoutParams<MarginLayoutParams> {
                    updateMargins(4.dp, 0, 4.dp, 4.dp)
                }
                binding.ivIcon.updateLayoutParams {
                    width = 40.dp
                    height = 40.dp
                }
                binding.root.updatePadding(top = 8.dp, bottom = 8.dp)
                binding.tvApplicationName.updateLayoutParams<MarginLayoutParams> {
                    marginStart = 8.dp
                }
                binding.root.setOnClickListener {
                    val pkgName = viewModel.selectedPackages.reversed()[adapterPosition]
                    viewModel.removedItem.value = pkgName
                    viewModel.selectedPackages.remove(pkgName)
                }
            }) { b, p, _ ->
            val pkgName = viewModel.selectedPackages[viewModel.selectedPackages.lastIndex - p]
            val info = viewModel.findPackageInfo(pkgName)
            b.tvExtraInfo.isVisible = false
            b.tvPkgName.isVisible = false
            b.tvApplicationName.text = info?.label ?: pkgName
            viewModel.iconLoader.loadIconTo(info, b.ivIcon, this)
        }
    }

    private val bottomActAdapter by lazy {
        inlineAdapter(
            viewModel.selectedActivities, ItemActivityInfoBinding::class.java,
            {
                binding.tvActivityName.isVisible = false
                binding.tvBadge.isVisible = false
                binding.root.updatePadding(bottom = 12.dp, top = 12.dp)
                binding.root.setOnClickListener {
                    val comp = viewModel.selectedActivities.reversed()[adapterPosition]
                    viewModel.removedItem.value = comp
                    viewModel.selectedActivities.remove(comp)
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
        if (viewModel.mode == MODE_PACKAGE) {
            binding.shoppingCart.rvBottom.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }
        binding.ibSortBy.setOnTouchListener(popupMenu.dragToOpenListener)
        binding.ibSortBy.setOnClickListener {
            popupMenu.show()
            popupMenu.menu.findItem(R.id.item_reverse_order).isChecked = viewModel.isOrderReversed
            popupMenu.menu.findItem(R.id.item_show_system_app).isChecked = viewModel.showSystemApps
        }
        shoppingCartIntegration.init(this)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.doOnItemSelected {
            viewModel.currentItem.value = it
        }
        binding.shoppingCart.btnCount.setOnClickListener {
            if (viewModel.selectedCount notEq 0)
                viewModel.showClearAllDialog.value = true
        }
        binding.shoppingCart.btnComplete.setOnClickListener {
            if (viewModel.complete()) {
                dismiss()
            } else {
                toast(R.string.nothing_selected)
            }
        }
        observeLiveData()
    }

    fun onPackageItemClicked(info: PackageInfoWrapper, binding: ItemApplicationInfoBinding) {
        if (viewModel.mode == MODE_PACKAGE) {
            val pkgName = info.packageName
            if (viewModel.selectedPackages.contains(pkgName)) {
                viewModel.removedItem.value = pkgName
                viewModel.selectedPackages.remove(pkgName)
            } else {
                viewModel.selectedPackages.add(pkgName)
                shoppingCartIntegration.animateIntoShopCart(binding.ivIcon, false)
                viewModel.addedItem.value = pkgName
            }
        } else {
            viewModel.selectedPackage.value = info
            this@ComponentSelectorDialog.binding.viewPager.currentItem = 1
        }
    }

    fun onActivityItemClicked(info: ActivityInfoWrapper, binding: ItemActivityInfoBinding) {
        val compName = info.componentName
        if (viewModel.selectedActivities.contains(compName)) {
            viewModel.removedItem.value = compName
            viewModel.selectedActivities.remove(compName)
        } else {
            viewModel.selectedActivities.add(compName)
            shoppingCartIntegration.animateIntoShopCart(binding.ivIcon, false)
            viewModel.addedItem.value = compName
        }
    }

    private fun observeLiveData() {
        val transition = ChangeBounds().addTarget(binding.shoppingCart.root)
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
        observeTransient(viewModel.removedItem) {
            if (viewModel.mode == MODE_PACKAGE) {
                val index = viewModel.selectedPackages.indexOf(it)
                bottomPkgAdapter.notifyItemRemoved(viewModel.selectedPackages.lastIndex - index)
            } else {
                val index = viewModel.selectedActivities.indexOf(it)
                bottomActAdapter.notifyItemRemoved(viewModel.selectedActivities.lastIndex - index)
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
    }

    override fun onBackPressed(): Boolean {
        if (viewModel.currentItem eq 1) {
            binding.viewPager.currentItem = 0
            return true
        }
        return super.onBackPressed()
    }
}