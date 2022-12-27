/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.util

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import top.xjunz.tasker.app

/**
 * The helper class for retrieving [PackageInfo]s.
 *
 * @author xjunz 2022/10/08
 */
object PackageInfoLoader {

    fun loadPackageInfo(pkgName: String, orFlags: Int = 0): PackageInfo? {
        val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES or orFlags
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.packageManager.getPackageInfo(
                    pkgName, PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getPackageInfo(pkgName, flags)
            }
        }.getOrNull()
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun loadAllPackages(): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            app.packageManager.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        }
    }
}