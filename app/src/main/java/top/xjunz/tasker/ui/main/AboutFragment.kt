/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.autostart.AutoStartUtil
import top.xjunz.tasker.databinding.FragmentAboutBinding
import top.xjunz.tasker.databinding.ItemMainOptionBinding
import top.xjunz.tasker.ktx.compress
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.observeError
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ktx.viewUrlSafely
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.service.isFloatingInspectorShown
import top.xjunz.tasker.service.isPremium
import top.xjunz.tasker.service.serviceController
import top.xjunz.tasker.task.storage.TaskStorage
import top.xjunz.tasker.task.storage.TaskStorage.X_TASK_FILE_ARCHIVE_SUFFIX
import top.xjunz.tasker.task.storage.TaskStorage.fileOnStorage
import top.xjunz.tasker.task.storage.TaskStorage.getFileName
import top.xjunz.tasker.ui.base.BaseFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.main.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.ui.purchase.PurchaseDialog
import top.xjunz.tasker.ui.purchase.PurchaseDialog.Companion.showPurchaseDialog
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.Feedbacks
import top.xjunz.tasker.util.formatCurrentTime
import java.util.zip.ZipOutputStream

/**
 * @author xjunz 2023/03/01
 */
class AboutFragment : BaseFragment<FragmentAboutBinding>(), ScrollTarget,
    ActivityResultCallback<Uri?> {

    private class InnerViewModel : ViewModel() {

        val onSaveToStorageError = MutableLiveData<Throwable>()

        fun saveTasksArchiveToStorage(contentResolver: ContentResolver, uri: Uri) {
            viewModelScope.async {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use {
                        ZipOutputStream(it).use { zip ->
                            TaskStorage.getAllTasks().forEach { task ->
                                zip.compress(task.fileOnStorage, task.getFileName(false))
                            }
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

    private val viewModel by viewModels<InnerViewModel>()

    private lateinit var saveToSAFLauncher: ActivityResultLauncher<String>

    private val adapter by lazy {
        inlineAdapter(MainOption.ALL_OPTIONS, ItemMainOptionBinding::class.java, {
            binding.root.setNoDoubleClickListener {
                onOptionClicked(it, MainOption.ALL_OPTIONS[adapterPosition])
            }
        }) { binding, _, option ->
            binding.tvTitle.text = option.title.text
            val desc = option.desc.invoke()
            binding.tvDesc.isVisible = desc != null
            if (desc != null) {
                binding.tvDesc.text = when (desc) {
                    is Int -> desc.text
                    is CharSequence -> desc
                    else -> desc.toString()
                }
            }
            binding.ivIcon.setImageResource(option.icon)
            binding.tvDesc2.isVisible = option.longDesc != -1
            if (option.longDesc != -1) {
                binding.tvDesc2.text = option.longDesc.text
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveToSAFLauncher =
            registerForActivityResult(object : ActivityResultContract<String, Uri?>() {
                override fun createIntent(context: Context, input: String): Intent {
                    return Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*").putExtra(Intent.EXTRA_TITLE, input)
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                    if (resultCode == Activity.RESULT_CANCELED) toast(R.string.cancelled)
                    return intent?.data
                }
            }, this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvOption.adapter = adapter
        val mvm = peekMainViewModel()
        observe(mvm.appbarHeight) {
            binding.rvOption.updatePadding(top = it)
        }
        observe(mvm.paddingBottom) {
            binding.rvOption.updatePadding(bottom = it)
        }
        observe(PremiumMixin.premiumStatusLiveData) {
            adapter.notifyItemChanged(
                MainOption.ALL_OPTIONS.indexOf(MainOption.PremiumStatus), true
            )
            adapter.notifyItemChanged(
                MainOption.ALL_OPTIONS.indexOf(MainOption.AutoStart), true
            )
        }
        observe(app.updateInfo) {
            adapter.notifyItemChanged(
                MainOption.ALL_OPTIONS.indexOf(MainOption.VersionInfo), true
            )
        }
        observeError(viewModel.onSaveToStorageError)
    }

    private fun onOptionClicked(view: View, option: MainOption) {
        fun updateOption() {
            adapter.notifyItemChanged(MainOption.ALL_OPTIONS.indexOf(option))
        }
        when (option) {
            MainOption.Feedback -> {
                val menu = PopupMenu(requireContext(), view, Gravity.END)
                menu.inflate(R.menu.feedbacks)
                menu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.item_feedback_email -> {
                            Feedbacks.feedbackByEmail(null)
                        }

                        R.id.item_feedback_group -> {
                            requireContext().viewUrlSafely("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=258644994&card_type=group&source=qrcode")
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
                menu.show()
            }

            MainOption.About -> {
                /* no-op */
            }

            MainOption.NightMode -> {
                val popupMenu = PopupMenu(requireContext(), view, Gravity.END)
                popupMenu.inflate(R.menu.night_modes)
                popupMenu.setOnMenuItemClickListener {
                    val mode = when (it.itemId) {
                        R.id.item_turn_on -> {
                            AppCompatDelegate.MODE_NIGHT_YES
                        }

                        R.id.item_turn_off -> {
                            AppCompatDelegate.MODE_NIGHT_NO
                        }

                        R.id.item_follow_system -> {
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }

                        else -> illegalArgument()
                    }
                    if (Preferences.nightMode != mode) {
                        AppCompatDelegate.setDefaultNightMode(mode)
                        if (isFloatingInspectorShown) {
                            floatingInspector.viewModel.onConfigurationChanged()
                        }
                        Preferences.nightMode = mode
                        updateOption()
                    }
                    return@setOnMenuItemClickListener true
                }
                popupMenu.show()
            }

            MainOption.PremiumStatus -> PurchaseDialog().show(childFragmentManager)
            MainOption.VersionInfo -> VersionInfoDialog().show(childFragmentManager)
            MainOption.AutoStart -> {
                if (!isPremium) {
                    showPurchaseDialog(R.string.tip_auto_start_need_premium)
                } else {
                    AutoStartUtil.toggleAutoStart(!AutoStartUtil.isAutoStartEnabled)
                    updateOption()
                }
            }

            MainOption.WakeLock -> {
                Preferences.enableWakeLock = !Preferences.enableWakeLock
                if (serviceController.isServiceRunning) {
                    if (Preferences.enableWakeLock) {
                        currentService.acquireWakeLock()
                    } else {
                        currentService.releaseWakeLock()
                    }
                }
                updateOption()
            }

            MainOption.ExportTasks -> {
                saveToSAFLauncher.launch(
                    R.string.format_task_archive_name.format(
                        formatCurrentTime(), X_TASK_FILE_ARCHIVE_SUFFIX
                    )
                )
            }
        }
    }

    override fun getScrollTarget(): RecyclerView? {
        return if (isAdded) binding.rvOption else null
    }

    override fun onActivityResult(result: Uri?) {
        if (result == null) return
        viewModel.saveTasksArchiveToStorage(requireActivity().contentResolver, result)
    }
}