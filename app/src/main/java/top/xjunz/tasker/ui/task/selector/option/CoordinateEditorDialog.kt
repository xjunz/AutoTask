/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.option

import android.graphics.Point
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.launch
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.DisplayManagerBridge
import top.xjunz.tasker.databinding.DialogCoordinateEditorBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2023/01/13
 */
class CoordinateEditorDialog : BaseDialogFragment<DialogCoordinateEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        var title: CharSequence? = null

        lateinit var onCompletion: (Int, Int) -> Unit
    }

    private val viewModel by viewModels<InnerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        peekMainViewModel().doOnAction(this, FloatingInspector.ACTION_SELECT_COORDINATE) {
            val point = IntValueUtil.parseCoordinate(it.toInt())
            binding.etX.setText(point.x.toString())
            binding.etY.setText(point.y.toString())
        }
    }

    fun init(
        title: CharSequence?,
        initial: Point?,
        onCompletion: (x: Int, y: Int) -> Unit
    ) = doWhenCreated {
        viewModel.title = title
        viewModel.onCompletion = onCompletion
        lifecycleScope.launch {
            lifecycle.withStarted {
                binding.etX.setText(initial?.x?.toString())
                binding.etY.setText(initial?.y?.toString())
            }
        }
    }

    private fun initViews() {
        binding.tvTitle.text = viewModel.title
        val size = DisplayManagerBridge.size
        binding.tvCaption.text = R.string.format_screen_width_height.format(size.x, size.y)
        binding.btnNegative.setOnClickListener {
            dismiss()
        }
        binding.ibDismiss.setOnClickListener {
            dismiss()
        }
        binding.etX.setOnEnterListener {
            binding.etX.clearFocus()
            binding.etY.requestFocus()
        }
        binding.etY.setOnEnterListener {
            binding.btnPositive.performClick()
        }
        binding.btnPositive.setOnClickListener {
            val x = binding.etX.textString.toIntOrNull()
            val y = binding.etY.textString.toIntOrNull()
            if (x == null) {
                binding.tilX.shake()
                binding.tilX.requestFocus()
                toast(R.string.format_not_specified.format(R.string.x_coordinate.str))
            } else if (y == null) {
                binding.tilY.shake()
                binding.tilY.requestFocus()
                toast(R.string.format_not_specified.format(R.string.y_coordinate.str))
            } else {
                viewModel.onCompletion(x, y)
                dismiss()
            }
        }
        binding.cvContainer.setAntiMoneyClickListener {
            FloatingInspectorDialog().setMode(InspectorMode.COORDS).show(childFragmentManager)
        }
    }
}