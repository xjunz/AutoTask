/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.common

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.databinding.DialogPreferenceHelpBinding
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.demo.Demonstration
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/12/02
 */
class PreferenceHelpDialog : BaseDialogFragment<DialogPreferenceHelpBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {
        lateinit var title: CharSequence
        lateinit var helpText: CharSequence
        var defCheckedState: Boolean = true

        lateinit var doOnConfirmed: (noMore: Boolean) -> Unit

        var demoInitializer: ((Context) -> Demonstration)? = null

        var show: Boolean = true
    }

    private lateinit var demonstration: Demonstration

    private val viewModel by viewModels<InnerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (viewModel.show) {
            super.onCreateView(inflater, container, savedInstanceState)
        } else {
            viewModel.doOnConfirmed(true)
            dismiss()
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = viewModel.title
        binding.tvCaption.text = viewModel.helpText
        binding.btnPositive.setAntiMoneyClickListener {
            viewModel.doOnConfirmed(binding.checkbox.isChecked)
            dismiss()
        }
        binding.btnNegative.setOnClickListener {
            dismiss()
        }
        if (viewModel.demoInitializer != null) {
            demonstration = viewModel.demoInitializer!!.invoke(requireContext())
            binding.container.isVisible = true
            binding.container.addView(
                demonstration.getView(),
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            binding.container.postDelayed(200L) {
                demonstration.startDemonstration()
            }
        }
        if (savedInstanceState == null)
            binding.checkbox.isChecked = viewModel.defCheckedState
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::demonstration.isInitialized)
            demonstration.stopDemonstration()
    }

    fun setDemonstration(block: (Context) -> Demonstration) = doWhenCreated {
        viewModel.demoInitializer = block
    }

    fun setDefaultCheckState(checked: Boolean) = doWhenCreated {
        viewModel.defCheckedState = checked
    }

    fun init(
        titleRes: Int,
        helpRes: Int,
        show: Boolean = true,
        onConfirmation: (noMore: Boolean) -> Unit
    ): PreferenceHelpDialog =
        doWhenCreated {
            viewModel.title = titleRes.text
            viewModel.helpText = helpRes.text
            viewModel.doOnConfirmed = onConfirmation
            viewModel.show = show
        }

}