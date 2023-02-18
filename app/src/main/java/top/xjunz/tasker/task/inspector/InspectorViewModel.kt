/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector

import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import androidx.test.uiautomator.PointerGesture
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.gesture.SerializableInputEvent

/**
 * @author xjunz 2022/10/13
 */
class InspectorViewModel {

    val toastText = MutableLiveData<CharSequence?>()

    val currentComponent = MutableLiveData<ComponentInfoWrapper>()

    val currentMode = MutableLiveData<InspectorMode>()

    val onKeyUpOrCancelled = MutableLiveData<Int>()

    val onKeyLongPressed = MutableLiveData<Int>()

    val onCoordinateSelected = MutableLiveData<Boolean>()

    val onComponentSelected = MutableLiveData<Boolean>()

    val isCollapsed = MutableLiveData(true)

    val showGamePad = MutableLiveData(false)

    val showNodeInfo = MutableLiveData(false)

    val showGrids = MutableLiveData(true)

    val pinScreenShot = MutableLiveData(true)

    val currentNodeTree = MutableLiveData<StableNodeInfo?>()

    val showNodeTree = MutableLiveData(false)

    val showGestures = MutableLiveData<Boolean>()

    val highlightNode = MutableLiveData<StableNodeInfo>()

    val isConfirmButtonEnabled = MutableLiveData(true)

    private val _recordedEvents = mutableListOf<SerializableInputEvent>()

    val recordedEvents = MutableLiveData(_recordedEvents)

    val isRecordingGesture = MutableLiveData<Boolean>()

    val requestRecordingState = MutableLiveData<Boolean>()

    val requestReplayGestures = MutableLiveData<Collection<SerializableInputEvent>>()

    val onGesturePlaybackStarted = MutableLiveData<PointerGesture>()

    val currentGesturePlaybackDuration = MutableLiveData<Long>()

    val onGesturePlaybackEnded = MutableLiveData<Boolean>()

    var windowWidth: Int = -1

    var windowHeight: Int = -1

    var bubbleX: Int = 0

    var bubbleY: Int = 0

    private var previousRecordTimestamp = -1L

    fun makeToast(any: Any?, post: Boolean = false) {
        when (any) {
            is Int -> toastText.value = any.text
            is CharSequence -> toastText.value = any
            else -> if (post) toastText.postValue(any.toString())
            else toastText.value = any.toString()
        }
    }

    fun onConfigurationChanged() {
        // Collapse the inspector and dismiss node info and node tree overlays
        isCollapsed.value = true
        showNodeInfo.value = false
        showNodeTree.value = false
    }

    fun recordGesture(gesture: PointerGesture) {
        _recordedEvents.add(SerializableInputEvent.wrap(gesture))
        previousRecordTimestamp = SystemClock.uptimeMillis()
    }

    fun recordKeyEvent(keyCode: Int) {
        if (previousRecordTimestamp == -1L) {
            _recordedEvents.add(SerializableInputEvent.wrap(keyCode))
        } else {
            _recordedEvents.add(
                SerializableInputEvent.wrap(
                    keyCode, SystemClock.uptimeMillis() - previousRecordTimestamp
                )
            )
        }
        previousRecordTimestamp = SystemClock.uptimeMillis()
    }

    fun clearAllRecordedEvents() {
        _recordedEvents.clear()
        previousRecordTimestamp = -1
        isConfirmButtonEnabled.value = false
    }
}