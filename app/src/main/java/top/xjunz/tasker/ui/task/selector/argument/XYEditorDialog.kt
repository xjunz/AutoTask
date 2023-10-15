/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.graphics.Point
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.launch
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.DisplayManagerBridge
import top.xjunz.tasker.databinding.DialogCoordinateEditorBinding
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.setOnEnterListener
import top.xjunz.tasker.ktx.setSelectionToEnd
import top.xjunz.tasker.ktx.shake
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.textString
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.VariantArgType
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.main.EventCenter.doOnEventRoutedWithValue
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2023/01/13
 */
class XYEditorDialog : BaseDialogFragment<DialogCoordinateEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        var type: Int = VariantArgType.INT_COORDINATE

        var title: CharSequence? = null

        var help: CharSequence? = null

        lateinit var onCompletion: (Int, Int) -> Unit

        @StringRes
        var hintX: Int = -1

        @StringRes
        var hintY: Int = -1
    }

    private val viewModel by viewModels<InnerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        doOnEventRoutedWithValue<Int>(FloatingInspector.EVENT_COORDINATE_SELECTED) {
            val point = IntValueUtil.parseXY(it)
            binding.etX.setText(point.x.toString())
            binding.etY.setText(point.y.toString())
            toast(R.string.coordinate_updated)
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
                showSoftInput(binding.etX)
                binding.etX.setSelectionToEnd()
            }
        }
    }

    fun setHelp(helpRes: CharSequence?) = doWhenCreated {
        viewModel.help = helpRes
    }

    fun setVariantType(variantType: Int, @StringRes hintX: Int, @StringRes hintY: Int) =
        doWhenCreated {
            viewModel.type = variantType
            viewModel.hintX = hintX
            viewModel.hintY = hintY
        }

    private fun initViews() {
        binding.tvTitle.text = viewModel.title
        if (viewModel.type == VariantArgType.INT_COORDINATE) {
            val size = DisplayManagerBridge.size
            binding.tvHelp.text = R.string.format_screen_width_height.format(size.x, size.y)
            binding.cvContainer.isVisible = true
        } else {
            binding.tilX.setHint(viewModel.hintX)
            binding.tilY.setHint(viewModel.hintY)
            binding.tvHelp.text = viewModel.help
        }
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
                toast(R.string.error_unspecified)
            } else if (y == null) {
                binding.tilY.shake()
                binding.tilY.requestFocus()
                toast(R.string.error_unspecified)
            } else {
                viewModel.onCompletion(x, y)
                dismiss()
            }
        }
        binding.cvContainer.setNoDoubleClickListener {
            FloatingInspectorDialog().setMode(InspectorMode.COORDS).show(childFragmentManager)
        }
    }
}