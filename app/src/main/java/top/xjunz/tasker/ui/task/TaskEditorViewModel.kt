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

    val selectedIndex = MutableLiveData(-1)

    val selectedItem = MutableLiveData<Applet>()
}