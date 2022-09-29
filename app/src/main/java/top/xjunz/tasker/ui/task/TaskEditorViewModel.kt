package top.xjunz.tasker.ui.task

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.engine.flow.Flow

/**
 * @author xjunz 2022/09/10
 */
class TaskEditorViewModel : ViewModel() {

    var isNewTask: Boolean = true

    var flow: Flow = Flow.defaultFlow()

    val selectedFlowIndex = MutableLiveData(-1)

    val selectedFlowItem = MutableLiveData<Applet>()

    val panelHeights = IntArray(2)

    val currentPage = MutableLiveData<Int>()
}