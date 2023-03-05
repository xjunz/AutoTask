/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.databinding.FragmentAboutBinding
import top.xjunz.tasker.databinding.ItemMainOptionBinding
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.ui.base.BaseFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.main.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.ui.purchase.PurchaseDialog
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.Feedbacks

/**
 * @author xjunz 2023/03/01
 */
class AboutFragment : BaseFragment<FragmentAboutBinding>(), ScrollTarget {

    private val adapter by lazy {
        inlineAdapter(MainOption.ALL_OPTIONS, ItemMainOptionBinding::class.java, {
            binding.root.setNoDoubleClickListener {
                onOptionClicked(it, MainOption.ALL_OPTIONS[adapterPosition])
            }
        }) { binding, _, option ->
            binding.tvTitle.text = option.title.text
            val desc = option.desc.invoke()
            binding.tvDesc.isVisible = desc != null
            if (desc != null) {
                binding.tvDesc.text = when (desc) {
                    is Int -> desc.text
                    is CharSequence -> desc
                    else -> desc.toString()
                }
            }
            binding.ivIcon.setImageResource(option.icon)
            binding.tvDesc2.isVisible = option.longDesc != -1
            if (option.longDesc != -1) {
                binding.tvDesc2.text = option.longDesc.text
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvOption.adapter = adapter
        val mvm = peekMainViewModel()
        observe(mvm.appbarHeight) {
            binding.rvOption.updatePadding(top = it)
        }
        observe(mvm.paddingBottom) {
            binding.rvOption.updatePadding(bottom = it)
        }
        observe(PremiumMixin.premiumStatusLiveData) {
            adapter.notifyItemChanged(
                MainOption.ALL_OPTIONS.indexOf(MainOption.PremiumStatus), true
            )
        }
        observe(app.updateInfo) {
            adapter.notifyItemChanged(
                MainOption.ALL_OPTIONS.indexOf(MainOption.VersionInfo), true
            )
        }
    }

    private fun onOptionClicked(view: View, option: MainOption) {
        when (option) {
            MainOption.Feedback -> Feedbacks.feedbackByEmail(null)
            MainOption.About -> {
                /* no-op */
            }
            MainOption.NightMode -> {
                val popupMenu = PopupMenu(requireContext(), view, Gravity.END)
                popupMenu.inflate(R.menu.night_modes)
                popupMenu.setOnMenuItemClickListener {
                    val mode = when (it.itemId) {
                        R.id.item_turn_on -> {
                            AppCompatDelegate.MODE_NIGHT_YES
                        }
                        R.id.item_turn_off -> {
                            AppCompatDelegate.MODE_NIGHT_NO
                        }
                        R.id.item_follow_system -> {
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                        else -> illegalArgument()
                    }
                    AppCompatDelegate.setDefaultNightMode(mode)
                    Preferences.nightMode = mode
                    adapter.notifyItemChanged(MainOption.ALL_OPTIONS.indexOf(option))
                    return@setOnMenuItemClickListener true
                }
                popupMenu.show()
            }
            MainOption.PremiumStatus -> PurchaseDialog().show(childFragmentManager)
            MainOption.VersionInfo -> VersionInfoDialog().show(childFragmentManager)
        }
    }

    override fun getScrollTarget(): RecyclerView? {
        return if (isAdded) binding.rvOption else null
    }
}