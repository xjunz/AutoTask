/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import kotlinx.coroutines.launch
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.*
import top.xjunz.tasker.databinding.ActivityMainBinding
import top.xjunz.tasker.engine.applet.util.hierarchy
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.service.isPremium
import top.xjunz.tasker.service.serviceController
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.task.runtime.LocalTaskManager.isEnabled
import top.xjunz.tasker.task.runtime.ResidentTaskScheduler
import top.xjunz.tasker.task.storage.TaskStorage
import top.xjunz.tasker.ui.base.DialogStackMixin
import top.xjunz.tasker.ui.purchase.PurchaseDialog.Companion.showPurchaseDialog
import top.xjunz.tasker.ui.service.ServiceStarterDialog
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.ui.task.showcase.*
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.ClickListenerUtil.setOnDoubleClickListener
import top.xjunz.tasker.util.ShizukuUtil
import java.util.*
import java.util.concurrent.TimeoutException

/**
 * @author xjunz 2021/6/20 21:05
 */
class MainActivity : AppCompatActivity(), DialogStackManager.Callback {

    private val mainViewModel by viewModels<MainViewModel>()

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val viewModel by viewModels<TaskShowcaseViewModel>()

    private val scrollTargets = arrayOfNulls<ScrollTarget>(4)

    private lateinit var fabBehaviour: HideBottomViewOnScrollBehavior<View>

    private val viewPagerAdapter by lazy {
        object : FragmentStateAdapter(this) {

            override fun getItemCount(): Int = 4

            override fun createFragment(position: Int): Fragment {
                val f = when (position) {
                    0 -> EnabledTaskFragment()
                    1 -> ResidentTaskFragment()
                    2 -> OneshotTaskFragment()
                    3 -> AboutFragment()
                    else -> illegalArgument()
                }
                scrollTargets[position] = f
                return f
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(Preferences.nightMode)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        app.setAppTheme(theme)
        setContentView(binding.root)
        initViews()
        observeData()
        initServiceController()
        DialogStackManager.setCallback(this)
        if (BuildConfig.DEBUG) {
            binding.tvTitle.setOnDoubleClickListener {
                PremiumMixin.clearPremium()
                toast("Premium cleared!")
            }
        }
        if (!Preferences.privacyPolicyAcknowledged) {
            PrivacyPolicyDialog().show(supportFragmentManager)
        }
        mainViewModel.checkForUpdates()
    }

    private var isExited = false

    override fun onDialogPush(stack: Stack<DialogStackManager.StackEntry>) {
        if (!DialogStackManager.isVisible(null) && !isExited) {
            DialogStackMixin.animateExit(window)
            isExited = true
        }
    }

    override fun onDialogPop(stack: Stack<DialogStackManager.StackEntry>) {
        if (DialogStackManager.isVisible(null) && isExited) {
            DialogStackMixin.animateReturn(window)
            isExited = false
        }
    }

    private fun initServiceController() {
        serviceController.setStateListener(mainViewModel)
        serviceController.bindExistingServiceIfExists()
    }

    override fun onResume() {
        super.onResume()
        serviceController.syncStatus()
    }

    private fun observeData() {
        observeTransient(viewModel.requestEditTask) {
            val task = it.first
            val prevChecksum = task.checksum
            FlowEditorDialog().initBase(task, false).doOnTaskEdited {
                viewModel.updateTask(prevChecksum, task)
            }.setFlowToNavigate(it.second?.hierarchy).show(supportFragmentManager)
        }
        observeTransient(viewModel.requestTrackTask) {
            FlowEditorDialog().initBase(it, true).setTrackMode().show(supportFragmentManager)
        }
        observeTransient(viewModel.requestToggleTask) { task ->
            when (task.metadata.taskType) {
                XTask.TYPE_RESIDENT -> {
                    val title = if (task.isEnabled) R.string.prompt_disable_task.text
                    else R.string.prompt_enable_task.text
                    makeSimplePromptDialog(msg = title) {
                        if (!task.isEnabled && !isPremium && LocalTaskManager.getEnabledResidentTasks().size ==
                            ResidentTaskScheduler.MAX_ENABLED_RESIDENT_TASKS_FOR_NON_PREMIUM_USER
                        ) {
                            showPurchaseDialog(R.string.tip_purchase_premium_max_resident_tasks)
                        } else {
                            viewModel.toggleTask(task)
                        }
                    }.setNeutralButton(
                        if (task.isEnabled) R.string.disable_all_tasks else R.string.enable_all_tasks
                    ) { _, _ ->
                        val tasks = TaskStorage.getAllTasks().filter {
                            it.metadata.taskType == XTask.TYPE_RESIDENT && it.isEnabled == task.isEnabled
                        }.toList()
                        if (!task.isEnabled && !isPremium && tasks.size + LocalTaskManager.getEnabledResidentTasks().size >
                            ResidentTaskScheduler.MAX_ENABLED_RESIDENT_TASKS_FOR_NON_PREMIUM_USER
                        ) {
                            showPurchaseDialog(R.string.tip_purchase_premium_max_resident_tasks)
                        } else {
                            tasks.forEach {
                                viewModel.toggleTask(it)
                            }
                        }
                    }.show().also {
                        it.getButton(DialogInterface.BUTTON_NEUTRAL)
                            .setTextColor(ColorScheme.colorError)
                    }
                }
                XTask.TYPE_ONESHOT -> {
                    FloatingInspectorDialog().setMode(InspectorMode.TASK_ASSISTANT).doOnSucceeded {
                        floatingInspector.viewModel.isCollapsed.value = false
                    }.show(supportFragmentManager)
                }
            }
        }
        observeDangerousConfirmation(
            viewModel.requestDeleteTask, R.string.prompt_delete_task, R.string.delete
        ) {
            viewModel.deleteRequestedTask()
        }
        observeTransient(viewModel.onNewTaskAdded) {
            when (it.metadata.taskType) {
                XTask.TYPE_ONESHOT -> binding.viewPager.currentItem = 2
                XTask.TYPE_RESIDENT -> binding.viewPager.currentItem = 1
            }
        }
        observeTransient(viewModel.requestAddNewTask) {
            viewModel.addRequestedTask()
        }
        observeTransient(viewModel.onTaskDeleted) {
            fabBehaviour.slideUp(binding.fabAction)
        }
        observe(mainViewModel.isServiceRunning) {
            binding.btnServiceControl.isActivated = it
            if (it) {
                binding.btnServiceControl.setText(R.string.stop_service)
                binding.btnServiceControl.setIconResource(R.drawable.ic_baseline_stop_24)
            } else {
                binding.btnServiceControl.setText(R.string.start_service)
                binding.btnServiceControl.setIconResource(R.drawable.ic_baseline_play_arrow_24)
            }
        }
        observeDialog(mainViewModel.serviceBindingError) {
            if (it is TimeoutException) {
                makeSimplePromptDialog(msg = R.string.prompt_shizuku_time_out).setTitle(R.string.error_occurred)
                    .setNegativeButton(R.string.launch_shizuku_manager) { _, _ ->
                        ShizukuUtil.launchShizukuManager()
                    }.setPositiveButton(R.string.retry) { _, _ ->
                        serviceController.bindService()
                    }.show()
            } else {
                showErrorDialog(it)
            }
        }
        observeDangerousConfirmation(
            mainViewModel.stopServiceConfirmation, R.string.prompt_stop_service, R.string.stop
        ) {
            mainViewModel.toggleService()
        }
        observe(PremiumMixin.premiumStatusLiveData) {
            if (it) {
                binding.tvTitle.setDrawableEnd(R.drawable.ic_verified_24px)
            } else {
                binding.tvTitle.setDrawableEnd(null)
            }
        }
        observe(app.updateInfo) {
            if (!isShell && it.hasUpdates() && mainViewModel.showUpdateDialog) {
                MaterialAlertDialogBuilder(this).setTitle(R.string.has_updates)
                    .setMessage(it.formatToString())
                    .setOnDismissListener {
                        mainViewModel.showUpdateDialog = false
                    }
                    .setOnCancelListener {
                        mainViewModel.showUpdateDialog = false
                    }
                    .setPositiveButton(R.string.download) { _, _ ->
                        viewUrlSafely("https://spark.appc02.com/tasker")
                    }.setNegativeButton(android.R.string.cancel, null).show()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {
        binding.tvTitle.setOnDoubleClickListener {
            Preferences.showDragToMoveTip = true
            Preferences.showToggleRelationTip = true
            Preferences.showSwipeToRemoveTip = true
            Preferences.showLongClickToSelectTip = true
        }
        binding.appBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
            v.doOnPreDraw {
                mainViewModel.appbarHeight.value = it.height
            }
        }
        binding.bottomBar.applySystemInsets { v, insets ->
            v.updatePadding(bottom = insets.bottom)
        }
        binding.fabAction.doOnPreDraw {
            mainViewModel.paddingBottom.value = it.height + binding.bottomBar.height
        }
        binding.bottomBar.background.let {
            it as MaterialShapeDrawable
            it.elevation = 4.dpFloat
            it.alpha = (.88 * 0xFF).toInt()
        }
        binding.btnServiceControl.setNoDoubleClickListener {
            if (it.isActivated) {
                mainViewModel.stopServiceConfirmation.value = true
            } else {
                ServiceStarterDialog().show(supportFragmentManager)
            }
        }
        binding.fabAction.setNoDoubleClickListener {
            TaskCreatorDialog().show(supportFragmentManager)
        }
        binding.viewPager.adapter = viewPagerAdapter
        binding.bottomBar.setOnItemSelectedListener {
            binding.viewPager.currentItem = binding.bottomBar.menu.indexOf(it)
            return@setOnItemSelectedListener true
        }
        fabBehaviour =
            ((binding.fabAction.layoutParams as CoordinatorLayout.LayoutParams).behavior as HideBottomViewOnScrollBehavior<View>)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomBar.selectedItemId = binding.bottomBar.menu.getItem(position).itemId
                ((binding.bottomBar.layoutParams as CoordinatorLayout.LayoutParams).behavior
                        as HideBottomViewOnScrollBehavior<View>).slideUp(binding.bottomBar, true)
                fabBehaviour.slideUp(binding.fabAction, true)
                binding.appBar.setLiftOnScrollTargetView(scrollTargets[position]?.getScrollTarget())
            }
        })
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // On old devices, onStart() may be called after onNewIntent(), hence there
        // will be no active observers.
        lifecycleScope.launch {
            lifecycle.withStarted {
                mainViewModel.onNewIntent.setValueIfObserved(intent.data to EventCenter.fetchTransientValue())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceController.unbindService()
        DialogStackManager.destroyAll()
        ColorScheme.release()
    }

}