/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.shared.ktx.insert
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.VibratorBridge
import top.xjunz.tasker.databinding.DialogVibrationPatternEditorBinding
import top.xjunz.tasker.databinding.ItemVibrationPatternBinding
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.indexOf
import top.xjunz.tasker.ktx.scrollPositionToCenterVertically
import top.xjunz.tasker.ktx.shake
import top.xjunz.tasker.ktx.textString
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.applet.action.Vibrate
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2023/10/26
 */
class VibrationPatternEditorDialog : BaseDialogFragment<DialogVibrationPatternEditorBinding>() {

    override val isFullScreen: Boolean
        get() = false

    private class InnerViewModel : ViewModel() {
        var vibrationDurations = mutableListOf(200L)
        var vibrationStrengths = mutableListOf(100)
        lateinit var doOnCompleted: (Vibrate.VibrationWaveForm) -> Unit
        var title: CharSequence? = null
    }

    private val viewModel by viewModels<InnerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPattern.adapter = VibrationPatternAdapter()
        binding.tvTitle.text = viewModel.title
        binding.btnCancel.setNoDoubleClickListener {
            dismiss()
        }
        binding.btnOk.setNoDoubleClickListener {
            getResult()?.let {
                viewModel.doOnCompleted(it)
                dismiss()
            }
        }
        binding.btnTest.setNoDoubleClickListener {
            getResult()?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibratorBridge.performVibrate(it)
                }
            }
        }
    }

    private fun getResult(): Vibrate.VibrationWaveForm? {
        for (i in 0..viewModel.vibrationDurations.lastIndex) {
            val d = viewModel.vibrationDurations[i]
            val s = viewModel.vibrationStrengths[i]
            if (d == -1L) {
                toast(R.string.error_vibration_duration)
                binding.rvPattern.scrollPositionToCenterVertically(i) {
                    it.shake()
                }
                return null
            }
            if (s !in 1..255) {
                toast(R.string.error_vibration_strength)
                binding.rvPattern.scrollPositionToCenterVertically(i) {
                    it.shake()
                }
                return null
            }
        }
        return Vibrate.VibrationWaveForm(
            viewModel.vibrationDurations.toLongArray(),
            viewModel.vibrationStrengths.toIntArray()
        )
    }

    fun init(
        pattern: Vibrate.VibrationWaveForm? = null,
        title: CharSequence?,
        onCompleted: (Vibrate.VibrationWaveForm) -> Unit
    ) = doWhenCreated {
        if (pattern != null) {
            viewModel.vibrationDurations = pattern.durations.toMutableList()
            viewModel.vibrationStrengths = pattern.strengths.toMutableList()
        }
        viewModel.title = title
        viewModel.doOnCompleted = onCompleted
    }

    private inner class VibrationPatternAdapter :
        RecyclerView.Adapter<VibrationPatternAdapter.VibrationPatternViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): VibrationPatternViewHolder {
            return VibrationPatternViewHolder(
                ItemVibrationPatternBinding.inflate(layoutInflater, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return viewModel.vibrationStrengths.size
        }

        override fun onBindViewHolder(holder: VibrationPatternViewHolder, position: Int) {
            holder.binding.let {
                val d = viewModel.vibrationDurations[position]
                it.etVibDuration.setText(if (d == -1L) null else d.toString())
                val s = viewModel.vibrationStrengths[position]
                it.etVibStrength.setText(if (s == -1) null else s.toString())
            }
        }

        private inner class VibrationPatternViewHolder(val binding: ItemVibrationPatternBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.etVibDuration.doAfterTextChanged {
                    viewModel.vibrationDurations[adapterPosition] =
                        binding.etVibDuration.textString.toLongOrNull() ?: -1
                }
                binding.etVibStrength.doAfterTextChanged {
                    viewModel.vibrationStrengths[adapterPosition] =
                        binding.etVibStrength.textString.toIntOrNull() ?: -1
                }
                binding.btnAdd.setNoDoubleClickListener {
                    val pos = adapterPosition
                    val popup =
                        androidx.appcompat.widget.PopupMenu(requireContext(), it, Gravity.CENTER)
                    popup.menu.add(R.string.add_before)
                    popup.menu.add(R.string.add_after)
                    popup.setOnMenuItemClickListener { item ->
                        when (popup.menu.indexOf(item)) {
                            0 -> {
                                viewModel.vibrationStrengths.insert(pos, -1)
                                viewModel.vibrationDurations.insert(pos, -1)
                                notifyItemInserted(pos)
                            }

                            1 -> {
                                viewModel.vibrationStrengths.add(-1)
                                viewModel.vibrationDurations.add(-1)
                                notifyItemInserted(pos + 1)
                            }
                        }
                        return@setOnMenuItemClickListener true
                    }
                    popup.show()
                }
                binding.btnDelete.setNoDoubleClickListener {
                    val pos = adapterPosition
                    if (itemCount > 1) {
                        viewModel.vibrationStrengths.removeAt(pos)
                        viewModel.vibrationDurations.removeAt(pos)
                        notifyItemRemoved(pos)
                    } else {
                        toast(R.string.error_not_removable)
                    }
                }
            }
        }
    }

}