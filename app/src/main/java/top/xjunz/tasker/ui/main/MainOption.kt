/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.autostart.AutoStartUtil
import top.xjunz.tasker.service.isPremium
import top.xjunz.tasker.upForGrabs

/**
 * @author xjunz 2023/02/27
 */
sealed class MainOption(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    var desc: () -> Any? = { null },
    @StringRes var longDesc: Int = -1
) {
    object PremiumStatus : MainOption(R.string.premium_status, R.drawable.ic_verified_24px, desc = {
        if (isPremium) {
            R.string.activated
        } else {
            R.string.not_activated
        }
    })

    object ExportTasks :
        MainOption(R.string.export_all_tasks, R.drawable.baseline_backup_24)

    object AutoStart :
        MainOption(R.string.auto_start_after_boot, R.drawable.baseline_restart_alt_24, desc = {
            if (AutoStartUtil.isAutoStartEnabled) {
                R.string.is_enabled
            } else {
                R.string.not_is_enabled
            }
        }, R.string.desc_auto_start)

    object WakeLock :
        MainOption(
            R.string.wake_lock,
            R.drawable.ic_outline_lock_24,
            desc = {
                if (Preferences.enableWakeLock) {
                    R.string.enabled
                } else {
                    R.string.not_is_enabled
                }
            },
            longDesc = R.string.tip_wake_lock
        )

    object NightMode : MainOption(R.string.night_mode, R.drawable.baseline_nights_stay_24, desc = {
        when (Preferences.nightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> R.string.turn_on
            AppCompatDelegate.MODE_NIGHT_NO -> R.string.turn_off
            else -> R.string.follow_system
        }
    })

    object Feedback : MainOption(R.string.feedback_and_communicate, R.drawable.baseline_chat_24)

    object VersionInfo : MainOption(R.string.version_info, R.drawable.baseline_info_24, desc = {
        if (app.updateInfo.value?.hasUpdates() == true) {
            R.string.new_version_detected
        } else {
            BuildConfig.VERSION_NAME
        }
    })

    object About : MainOption(
        R.string.about,
        R.drawable.ic_baseline_more_vert_24,
        longDesc = R.string.more_to_say
    )

    companion object {
        val ALL_OPTIONS = if (upForGrabs) {
            arrayOf(ExportTasks, AutoStart, WakeLock, NightMode, Feedback, VersionInfo, About)
        } else {
            arrayOf(PremiumStatus, ExportTasks, AutoStart, NightMode, Feedback, VersionInfo, About)
        }
    }
}