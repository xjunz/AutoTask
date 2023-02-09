/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.model

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import top.xjunz.tasker.app
import top.xjunz.tasker.bridge.ContextBridge
import java.io.File

/**
 * @author xjunz 2021/10/1
 */
class PackageInfoWrapper(val source: PackageInfo) {

    companion object {
        const val SORT_BY_LABEL = 0
        const val SORT_BY_SUSPICION = 1
        const val SORT_BY_SIZE = 2
        const val SORT_BY_FIRST_INSTALL_TIME = 3

        fun PackageInfo.wrapped(): PackageInfoWrapper {
            return PackageInfoWrapper(this)
        }
    }

    var selectedActCount: Int = 0

    val label: CharSequence by lazy {
        source.applicationInfo.loadLabel(ContextBridge.getContext().packageManager)
    }

    var packageName: String = source.packageName

    val entranceName: String? by lazy {
        app.packageManager.getLaunchIntentForPackage(source.packageName)?.component?.className
    }

    fun isSystemApp() = source.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            source.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

    private var suspicion = -1L

    val apkSize by lazy {
        File(source.applicationInfo.sourceDir).length()
    }

    /**
     * You sussy baka!
     */
    fun loadSuspicion(): Long {
        if (suspicion != -1L) {
            return suspicion
        }
        if (isSystemApp()) {
            suspicion = 0L
            return 0
        }
        suspicion = label.count { it.code in 0..127 }.toLong()
        when {
            suspicion == 0L -> suspicion = apkSize / (1024 * 1024 * 10)
            suspicion.toInt() != label.length -> {
                suspicion = label.length - suspicion
                return suspicion
            }
            else -> {
                suspicion = 1
                return suspicion
            }
        }
        suspicion += source.lastUpdateTime / (1000 * 60 * 60 * 24 * 30L)
        return suspicion
    }


    override fun hashCode(): Int {
        return packageName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PackageInfoWrapper

        if (packageName != other.packageName) return false

        return true
    }
}
