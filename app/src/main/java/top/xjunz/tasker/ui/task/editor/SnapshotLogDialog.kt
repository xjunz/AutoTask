/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskLogBinding
import top.xjunz.tasker.engine.task.TaskSnapshot
import top.xjunz.tasker.ktx.applySystemInsets
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.observeConfirmation
import top.xjunz.tasker.ktx.observeError
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.formatCurrentTime

/**
 * @author xjunz 2023/03/15
 */
class SnapshotLogDialog : BaseDialogFragment<DialogTaskLogBinding>(),
    ActivityResultCallback<Uri?> {

    private class InnerViewModel : ViewModel() {

        lateinit var taskName: String

        lateinit var snapshot: TaskSnapshot

        val showClearLogConfirmation = MutableLiveData<Boolean>()

        val onSaveToStorageError = MutableLiveData<Throwable>()

        fun saveLogToStorage(contentResolver: ContentResolver, uri: Uri) {
            viewModelScope.async {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use {
                        it.bufferedWriter().apply {
                            write(snapshot.log)
                            flush()
                        }
                    }
                }
                toast(R.string.saved_to_storage)
            }.invokeOnCompletion {
                if (it != null && it !is CancellationException) {
                    onSaveToStorageError.postValue(it)
                }
            }
        }
    }

    private lateinit var saveToSAFLauncher: ActivityResultLauncher<String>

    private val viewModel by viewModels<InnerViewModel>()

    fun setSnapshot(taskName: String, snapshot: TaskSnapshot) = doWhenCreated {
        viewModel.snapshot = snapshot
        viewModel.taskName = taskName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveToSAFLauncher =
            registerForActivityResult(object : ActivityResultContract<String, Uri?>() {
                override fun createIntent(context: Context, input: String): Intent {
                    return Intent.createChooser(
                        Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("*/*").putExtra(Intent.EXTRA_TITLE, input),
                        R.string.select_export_path.str
                    )
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                    if (resultCode == Activity.RESULT_CANCELED) toast(R.string.cancelled)
                    return intent?.data
                }
            }, this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lineCounter.bindTo(binding.etLog)
        binding.ibDismiss.setNoDoubleClickListener {
            dismiss()
        }
        binding.ibClear.setNoDoubleClickListener {
            viewModel.showClearLogConfirmation.value = true
        }
        binding.appBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }

        binding.ibSave.setNoDoubleClickListener {
            saveToSAFLauncher.launch(
                R.string.format_snapshot_filename.format(
                    viewModel.taskName, formatCurrentTime()
                )
            )
            toast(R.string.tip_select_log_save_dir)
        }
        binding.etLog.setText(viewModel.snapshot.log)
        binding.scrollView.doOnPreDraw {
            binding.etLog.minimumHeight = it.height
        }
        observeConfirmation(
            viewModel.showClearLogConfirmation,
            R.string.prompt_clear_snapshot_log
        ) {
            viewModel.snapshot.log = null
            LocalTaskManager.clearLog(viewModel.snapshot.checksum, viewModel.snapshot.id)
            dismiss()
        }
        observeError(viewModel.onSaveToStorageError)
    }

    override fun onActivityResult(result: Uri?) {
        if (result != null) {
            viewModel.saveLogToStorage(requireActivity().contentResolver, result)
        }
    }
}