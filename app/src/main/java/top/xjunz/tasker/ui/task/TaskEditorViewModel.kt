package top.xjunz.tasker.ui.task

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow

/**
 * @author xjunz 2022/09/10
 */
class TaskEditorViewModel : ViewModel() {

    var isNewTask: Boolean = true

    var flow: Flow = Flow.defaultFlow()

    val selectedFlowIndex = MutableLiveData(-1)

    val selectedFlowItem = MutableLiveData<Applet>()

    val currentPage = MutableLiveData<Int>()
}