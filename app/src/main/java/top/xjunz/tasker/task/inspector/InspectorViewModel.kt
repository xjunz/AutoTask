/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector

import androidx.lifecycle.MutableLiveData
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.task.applet.flow.ComponentInfoWrapper

/**
 * @author xjunz 2022/10/13
 */
class InspectorViewModel {

    var shouldAnimateItems: Boolean = true

    val toastText = MutableLiveData<CharSequence?>()

    val currentComp = MutableLiveData<ComponentInfoWrapper>()

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

    val highlightNode = MutableLiveData<StableNodeInfo>()

    var windowWidth: Int = -1

    var windowHeight: Int = -1

    var bubbleX: Int = 0

    var bubbleY: Int = 0

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
}