/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.option

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTimeIntervalEditorBinding
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.setMaxLength
import top.xjunz.tasker.ktx.textString
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/12/22
 */
class TimeIntervalEditorDialog : BaseDialogFragment<DialogTimeIntervalEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {
        var title: CharSequence? = null
        var initial: Int = 0
        lateinit var doOnCompletion: (Int) -> Unit
    }

    private val viewModel by viewModels<InnerViewModel>()

    fun init(title: CharSequence?, initial: Int, onCompletion: (Int) -> Unit) = doWhenCreated {
        viewModel.title = title
        viewModel.initial = initial
        viewModel.doOnCompletion = onCompletion
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val min = viewModel.initial / (60 * 1000)
            val sec = viewModel.initial % (60 * 1000) / 1000
            val mill = viewModel.initial % 1000
            binding.etMills.setText("$mill")
            binding.etSecond.setText("$sec")
            binding.etMin.setText("$min")
        }
        binding.tvTitle.text = viewModel.title
        binding.btnNegative.setOnClickListener {
            dismiss()
        }
        binding.etMin.setMaxLength(2)
        binding.etMills.setMaxLength(3)
        binding.etSecond.setMaxLength(2)
        binding.btnPositive.setAntiMoneyClickListener {
            val min = binding.etMin.textString.toIntOrNull() ?: 0
            val sec = binding.etSecond.textString.toIntOrNull() ?: 0
            val mill = binding.etMills.textString.toIntOrNull() ?: 0
            if (min == 0 && sec == 0 && mill == 0) {
                toastAndShake(R.string.error_require_positive_num)
                return@setAntiMoneyClickListener
            }
            viewModel.doOnCompletion(min * 60 * 1000 + sec * 1000 + mill)
            dismiss()
        }
        binding.ibDismiss.setOnClickListener {
            dismiss()
        }
    }
}