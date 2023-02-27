/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.ui.model.PackageInfoWrapper.Companion.wrapped

/**
 * @author xjunz 2023/01/06
 */
object PackageManagerBridge {

    private val packageManager by lazy {
        ContextBridge.getContext().packageManager
    }

    fun getLaunchIntentFor(pkgName: String): Intent? {
        return packageManager.getLaunchIntentForPackage(pkgName)
    }

    fun getLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName
    }

    fun loadPackageInfo(pkgName: String, orFlags: Int = 0): PackageInfo? {
        val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES or orFlags
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    pkgName, PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(pkgName, flags)
            }
        }.onFailure {
            it.logcatStackTrace()
        }.getOrNull()
    }

    fun loadLabelOfPackage(pkgName: String): CharSequence {
        return loadPackageInfo(pkgName)?.wrapped()?.label ?: pkgName
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun loadAllPackages(): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        }
    }

    fun isActivityExistent(pkgName: String, actName: String): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getActivityInfo(
                    ComponentName(pkgName, actName), PackageManager.ComponentInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getActivityInfo(ComponentName(pkgName, actName), 0)
            }
        }.isSuccess
    }
}