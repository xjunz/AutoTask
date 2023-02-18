/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.xjunz.tasker.databinding.DialogGesturesEditorBinding
import top.xjunz.tasker.databinding.ItemGestureBinding
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.task.gesture.SerializableInputEvent
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2023/02/17
 */
class GesturesEditorDialog : BaseDialogFragment<DialogGesturesEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        val gestures = MutableLiveData<MutableList<SerializableInputEvent>>()

        lateinit var onCompleted: (List<SerializableInputEvent>?) -> Unit
    }

    private val vm by viewModels<InnerViewModel>()

    private val adapter by lazy {
        inlineAdapter(
            vm.gestures.require(), ItemGestureBinding::class.java, {
                binding.btnPlayback.isVisible = false
            }) { binding, pos, gesture ->
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCancel.setAntiMoneyClickListener {
            dismiss()
        }
        binding.btnComplete.setAntiMoneyClickListener {
            vm.onCompleted(vm.gestures.value)
        }
        binding.btnOpen.setAntiMoneyClickListener {
            FloatingInspectorDialog().setMode(InspectorMode.GESTURE_RECORDER).doOnSucceeded {
                floatingInspector.viewModel.clearAllRecordedEvents()
                floatingInspector.viewModel.recordedEvents.require().addAll(vm.gestures.require())
                floatingInspector.viewModel.showGestures.value = true
            }.show(childFragmentManager)
        }
        binding.btnRecord.setAntiMoneyClickListener {
            FloatingInspectorDialog().setMode(InspectorMode.GESTURE_RECORDER)
                .show(childFragmentManager)
        }
        observe(vm.gestures) {
            binding.rvGestures.adapter = adapter
            binding.btnOpen.isEnabled = true
        }
    }

    fun init(
        flattened: String?,
        doOnCompleted: (List<SerializableInputEvent>?) -> Unit
    ) = doWhenCreated {
        if (flattened == null) return@doWhenCreated
        lifecycleScope.launch(Dispatchers.Default) {
            vm.onCompleted = doOnCompleted
            // vm.gestures.value = SamplingGesture.inflateFromString(flattened).toMutableList()
        }
    }

}