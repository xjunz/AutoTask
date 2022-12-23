package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView.Adapter
import kotlinx.coroutines.async
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogPreloadTasksBinding
import top.xjunz.tasker.databinding.ItemPreloadTaskBinding
import top.xjunz.tasker.ktx.invokeOnError
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ktx.toastUnexpectedError
import top.xjunz.tasker.task.TaskStorage
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/12/21
 */
class PreloadTaskDialog : BaseBottomSheetDialog<DialogPreloadTasksBinding>() {

    private class InnerViewModel : ViewModel() {

        val taskLoaded = MutableLiveData<Boolean>()

        fun preloadTasks() {
            if (TaskStorage.preloadTaskLoaded) {
                taskLoaded.value = true
            } else viewModelScope.async {
                TaskStorage.preloadTasks(AppletOptionFactory())
                TaskStorage.preloadTaskLoaded = true
                taskLoaded.value = true
            }.invokeOnError {
                toastUnexpectedError(it)
            }
        }
    }

    private val parentViewModel by lazy {
        requireParentFragment().viewModels<TaskShowcaseViewModel>().value
    }

    private val viewModel by viewModels<InnerViewModel>()

    private val adapter: Adapter<*> by lazy {
        inlineAdapter(TaskStorage.preloadTasks, ItemPreloadTaskBinding::class.java, {
            binding.btnAdd.setAntiMoneyClickListener {
                parentViewModel.requestAddNewTask.value = TaskStorage.preloadTasks[adapterPosition]
            }
        }) { binding, _, task ->
            binding.btnAdd.isEnabled = !TaskStorage.allTasks.contains(task)
            if (binding.btnAdd.isEnabled) {
                binding.btnAdd.text = R.string.add.text
            } else {
                binding.btnAdd.text = R.string.added.text
            }
            binding.tvTaskName.text = task.metadata.title
            binding.tvTaskDesc.text = task.metadata.description
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.preloadTasks()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeTransient(viewModel.taskLoaded) {
            binding.rvTaskList.adapter = adapter
        }
        observeTransient(parentViewModel.onNewTaskAdded) {
            adapter.notifyItemChanged(TaskStorage.preloadTasks.indexOf(it), true)
        }
    }
}