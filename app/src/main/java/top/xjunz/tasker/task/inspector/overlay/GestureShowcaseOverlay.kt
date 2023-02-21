/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.material.snackbar.Snackbar
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemGestureBinding
import top.xjunz.tasker.databinding.OverlayGestureShowcaseBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.gesture.SerializableInputEvent
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.main.EventCenter
import top.xjunz.tasker.ui.main.EventCenter.doOnEventReceived
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.formatMinSecMills
import java.util.*

/**
 * @author xjunz 2023/02/15
 */
class GestureShowcaseOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayGestureShowcaseBinding>(inspector) {

    private var currentEdition: SerializableInputEvent? = null

    private val events: MutableList<SerializableInputEvent> get() = vm.recordedEvents.require()

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.flags = base.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.height = WindowManager.LayoutParams.MATCH_PARENT
    }

    private val adapter: Adapter<*> by lazy {
        inlineAdapter(
            events, ItemGestureBinding::class.java, {
                binding.root.setNoDoubleClickListener {
                    currentEdition = events[adapterPosition]
                    this@GestureShowcaseOverlay.binding.apply {
                        touchBlocker.isVisible = true
                        container.blockTouch()
                        animateShow(containerEditor)
                        etGestureName.setText(currentEdition?.label)
                        etGestureDelay.setText(currentEdition?.delay?.toString())
                        etGestureName.setSelectionToEnd()
                        etGestureDelay.setSelectionToEnd()
                        etGestureName.requestFocus()
                    }
                }
                binding.btnDelete.setNoDoubleClickListener {
                    if (events.size == 1) {
                        binding.root.shake()
                        return@setNoDoubleClickListener
                    }
                    this@GestureShowcaseOverlay.binding.container.beginAutoTransition()
                    val pos = adapterPosition
                    val removed = events.removeAt(pos)
                    adapter.notifyItemRemoved(pos)
                    Snackbar.make(
                        this@GestureShowcaseOverlay.binding.rvGestures,
                        R.string.removed, Snackbar.LENGTH_SHORT
                    ).setAction(R.string.undo) {
                        this@GestureShowcaseOverlay.binding.container.beginAutoTransition()
                        if (pos > events.lastIndex) {
                            events.add(removed)
                        } else {
                            events.add(pos, removed)
                        }
                        adapter.notifyItemInserted(pos)
                    }.show()
                }
                binding.btnPlayback.setNoDoubleClickListener {
                    vm.showGestures.value = false
                    val event = events[adapterPosition]
                    val noDelay = SerializableInputEvent.wrap(event.getGesture().noDelay())
                    vm.requestReplayGestures.value = Collections.singleton(noDelay)
                }
            }) { binding, _, event ->
            if (event.type == SerializableInputEvent.INPUT_TYPE_KEY) {
                if (event.label != null) {
                    binding.tvName.text = event.label
                } else {
                    binding.tvName.setText(
                        when (event.getKeyCode()) {
                            KeyEvent.KEYCODE_BACK -> R.string.press_back
                            KeyEvent.KEYCODE_HOME -> R.string.press_home
                            KeyEvent.KEYCODE_VOLUME_UP -> R.string.press_volume_up
                            KeyEvent.KEYCODE_VOLUME_DOWN -> R.string.press_volume_down
                            else -> View.NO_ID
                        }
                    )
                }
                binding.tvDesc.text =
                    R.string.format_gesture_delay.format(formatMinSecMills(event.delay))
            } else {
                binding.tvName.text = event.label ?: R.string.perform_gesture.str
                binding.tvDesc.text = if (event.delay == 0L) {
                    R.string.format_gesture_duration.format(
                        formatMinSecMills(event.getGesture().duration())
                    )
                } else {
                    R.string.format_gesture_desc.format(
                        formatMinSecMills(event.getGesture().delay()),
                        formatMinSecMills(event.getGesture().duration())
                    )
                }
            }
        }
    }

    private val imm by lazy {
        context.getSystemService(InputMethodManager::class.java)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOverlayInflated() {
        super.onOverlayInflated()
        binding.touchBlocker.setOnClickListener {
            binding.btnEditorCancel.performClick()
        }
        binding.container.background = context.createMaterialShapeDrawable()
        binding.containerEditor.background = context.createMaterialShapeDrawable()
        binding.rvGestures.adapter = adapter
        binding.btnCancel.setNoDoubleClickListener {
            vm.showGestures.value = false
        }
        binding.btnComplete.setNoDoubleClickListener {
            EventCenter.routeEvent(
                FloatingInspector.EVENT_GESTURES_RECORDED, ArrayList(vm.recordedEvents.require())
            )
            vm.clearAllRecordedEvents()
            vm.showGestures.value = false
        }
        binding.btnReplay.setNoDoubleClickListener {
            vm.showGestures.value = false
            vm.requestReplayGestures.value = vm.recordedEvents.require()
        }
        binding.btnEditorComplete.setNoDoubleClickListener {
            if (binding.etGestureDelay.textString.isEmpty()) {
                vm.makeToast(R.string.error_empty_input)
                binding.etGestureDelay.shake()
                return@setNoDoubleClickListener
            }
            currentEdition?.label = binding.etGestureName.textString.ifEmpty { null }
            currentEdition?.delay = binding.etGestureDelay.textString.toLong()
            adapter.notifyItemChanged(events.indexOf(currentEdition))
            currentEdition = null
            binding.btnEditorCancel.performClick()
        }
        binding.btnEditorCancel.setNoDoubleClickListener {
            animateHide(binding.containerEditor)
            imm.hideSoftInputFromWindow(rootView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            binding.touchBlocker.isVisible = false
            binding.container.unblockTouch()
            binding.etGestureName.text = null
            binding.etGestureDelay.text = null
        }
        inspector.observe(vm.showGestures) {
            if (it) {
                if (events.isEmpty()) {
                    vm.makeToast(R.string.error_no_gesture_recorded)
                } else {
                    animateShow()
                    adapter.notifyDataSetChanged()
                }
            } else {
                animateHide()
            }
        }
        inspector.doOnEventReceived<List<SerializableInputEvent>>(FloatingInspector.EVENT_REQUEST_EDIT_GESTURES) {
            vm.clearAllRecordedEvents()
            vm.recordedEvents.require().addAll(it)
            if (it.isNotEmpty()) {
                vm.showGestures.value = true
                vm.isConfirmButtonEnabled.value = true
            }
        }
    }
}