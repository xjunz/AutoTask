/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import rikka.shizuku.Shizuku
import top.xjunz.tasker.FEEDBACK_GROUP_URL
import top.xjunz.tasker.R
import top.xjunz.tasker.autostart.AutoStartUtil
import top.xjunz.tasker.databinding.ActivityMainBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.OperatingMode
import top.xjunz.tasker.service.controller.ShizukuAutomatorServiceController
import top.xjunz.tasker.service.serviceController
import top.xjunz.tasker.ui.AboutFragment
import top.xjunz.tasker.ui.check.AvailabilityCheckDialog
import top.xjunz.tasker.ui.task.showcase.TaskShowcaseDialog
import top.xjunz.tasker.util.ShizukuUtil
import java.util.concurrent.TimeoutException

/**
 * @author xjunz 2021/6/20 21:05
 */
class MainActivity : AppCompatActivity(), Shizuku.OnRequestPermissionResultListener {

    private val viewModel by viewModels<MainViewModel>()

    private val handler by lazy {
        Handler(mainLooper)
    }

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        initViews()
        observeData()
        initServiceController()
        viewModel.init()
    }

    private fun initServiceController() {
        serviceController.setStateListener(viewModel)
        serviceController.bindExistingServiceIfExists()
    }

    private val popupMenu by lazy {
        PopupMenu(this, binding.ibMenu).apply {
            menuInflater.inflate(R.menu.main, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.item_feedback_group -> viewUrlSafely(FEEDBACK_GROUP_URL)
                    R.id.item_feedback_issues -> viewUrlSafely("https://github.com/xjunz/AutoSkip/issues")
                    R.id.item_about -> AboutFragment().show(supportFragmentManager, "about")
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private val operatingModeMenu by lazy {
        PopupMenu(this, binding.containerOperatingMode, Gravity.END).apply {
            menuInflater.inflate(R.menu.operating_modes, menu)
            setOnMenuItemClickListener {
                if (it.itemId == R.id.item_shizuku_mode) {
                    viewModel.setCurrentOperatingMode(OperatingMode.Shizuku)
                } else if (it.itemId == R.id.item_a11y_mode) {
                    viewModel.setCurrentOperatingMode(OperatingMode.Accessibility)
                }
                // Some status may have changed(such as shizuku-related status), sync now
                serviceController.syncStatus()
                return@setOnMenuItemClickListener true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceController.syncStatus()
    }

    private fun observeData() {
        observe(viewModel.isServiceBinding) {
            binding.btnRun.isEnabled = !it
            if (it) binding.tvServiceStatus.setText(R.string.service_connecting)
        }
        observe(viewModel.isServiceRunning) {
            binding.btnRun.isActivated = it
            if (it) {
                handler.post(updateDurationTask)
                binding.tvServiceStatus.setText(R.string.service_is_running)
                binding.btnRun.setText(R.string.stop)
            } else {
                handler.removeCallbacks(updateDurationTask)
                binding.tvServiceStatus.setText(R.string.service_not_started)
                binding.btnRun.setText(R.string.run)
                binding.tvPrompt.setText(R.string.prompt_start_service)
            }
        }
        observe(ShizukuAutomatorServiceController.isShizukuInstalled) {
            binding.btnShizukuAction.isActivated = it
            if (it) {
                binding.btnShizukuAction.setText(R.string.launch_shizuku_manager)
            } else {
                binding.btnShizukuAction.setText(R.string.install_shizuku)
            }
        }
        observe(ShizukuAutomatorServiceController.isShizukuGranted) {
            binding.btnGrant.isEnabled = Shizuku.pingBinder() && !it
            binding.btnRun.isEnabled = it
            if (it) {
                // [isRunning] has a higher priority
                if (viewModel.isServiceRunning.isNotTrue) {
                    binding.tvPrompt.setText(R.string.prompt_start_service)
                }
                binding.btnGrant.setText(R.string.granted)
            } else {
                binding.tvPrompt.setText(R.string.prompt_request_permission)
                binding.btnGrant.setText(R.string.grant)
            }
        }
        observeDialog(viewModel.serviceBindingError) {
            if (it is TimeoutException) {
                makeSimplePromptDialog(msg = R.string.prompt_shizuku_time_out)
                    .setTitle(R.string.error_occurred)
                    .setNegativeButton(R.string.launch_shizuku_manager) { _, _ ->
                        ShizukuUtil.launchShizukuManager()
                    }.setPositiveButton(R.string.retry) { _, _ ->
                        serviceController.bindService()
                    }.show()
            } else {
                showErrorDialog(it)
            }
        }
        observe(viewModel.operatingMode) {
            binding.root.beginAutoTransition()
            if (it == OperatingMode.Shizuku) {
                operatingModeMenu.menu.findItem(R.id.item_shizuku_mode).isChecked = true
            } else {
                operatingModeMenu.menu.findItem(R.id.item_a11y_mode).isChecked = true
            }
            binding.containerShizukuIntro.isVisible = it == OperatingMode.Shizuku
            binding.tvDescMode.text = it.description
            binding.tvMode.text = it.name
            binding.btnRun.isEnabled = it == OperatingMode.Accessibility
        }
        observeDangerousConfirmation(
            viewModel.stopServiceConfirmation,
            R.string.prompt_stop_service,
            R.string.stop
        ) {
            viewModel.toggleService()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {
        binding.ibMenu.setOnTouchListener(popupMenu.dragToOpenListener)
        binding.ibMenu.setOnClickListener {
            popupMenu.menu.findItem(R.id.item_auto_start).isChecked =
                AutoStartUtil.isAutoStartEnabled
            popupMenu.show()
        }
        binding.topBar.oneShotApplySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
            v.doOnPreDraw {
                binding.scrollView.updatePadding(top = v.height)
            }
        }
        binding.btnShizukuAction.setOnClickListener {
            if (ShizukuAutomatorServiceController.isShizukuInstalled.isTrue) {
                ShizukuUtil.launchShizukuManager()
            } else {
                viewUrlSafely("https://www.coolapk.com/apk/moe.shizuku.privileged.api")
            }
        }
        binding.btnGrant.setOnClickListener {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                toast(getString(R.string.pls_grant_manually))
                ShizukuUtil.launchShizukuManager()
            } else {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.addRequestPermissionResultListener(this)
                    Shizuku.requestPermission(1)
                } else {
                    ShizukuAutomatorServiceController.isShizukuGranted.value = true
                }
            }
        }
        binding.containerOperatingMode.setOnClickListener {
            if (viewModel.isServiceBinding.isTrue || viewModel.isServiceRunning.isTrue) {
                toast(R.string.prompt_unable_to_switch_mode)
                return@setOnClickListener
            }
            if (viewModel.operatingMode eq OperatingMode.Shizuku) {
                operatingModeMenu.menu.findItem(R.id.item_shizuku_mode).isChecked = true
            } else if (viewModel.operatingMode eq OperatingMode.Accessibility) {
                operatingModeMenu.menu.findItem(R.id.item_a11y_mode).isChecked = true
            }
            operatingModeMenu.show()
        }
        binding.btnRun.setOnClickListener {
            if (viewModel.isServiceRunning.isTrue) {
                viewModel.stopServiceConfirmation.toggle()
            } else {
                viewModel.toggleService()
            }
        }
        binding.containerCheck.setOnClickListener {
            if (viewModel.isServiceRunning.isNotTrue) {
                toast(R.string.pls_start_service)
                return@setOnClickListener
            }
            AvailabilityCheckDialog().show(supportFragmentManager)
        }
        binding.containerCustom.setOnClickListener {
            TaskShowcaseDialog().show(supportFragmentManager)
        }
    }

    private val updateDurationTask = object : Runnable {
        override fun run() {
            val timestamp = serviceController.startTimestamp
            if (timestamp > 0) {
                val duration = System.currentTimeMillis() - timestamp
                binding.tvPrompt.text = R.string.format_running_duration.format(
                    duration / 3_600_000, duration / 60_000 % 60, duration / 1000 % 60
                )
            }
            handler.postDelayed(this, 1000)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onNewIntent.setValueIfObserved(intent.data to EventCenter.fetchTransientValue())
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        serviceController.unbindService()
        DialogStackManager.destroyAll()
        Shizuku.removeRequestPermissionResultListener(this)
        ColorScheme.release()
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        ShizukuAutomatorServiceController.isShizukuGranted.value = ShizukuUtil.isShizukuAvailable
    }
}