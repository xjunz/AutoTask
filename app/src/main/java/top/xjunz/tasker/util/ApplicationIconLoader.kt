/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.util

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.core.util.lruCache
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.xjunz.tasker.R
import top.xjunz.tasker.ui.model.PackageInfoWrapper

/**
 * @author xjunz 2022/10/07
 */
class ApplicationIconLoader {

    private val cache = lruCache<String, Bitmap>(50)

    private fun loadIcon(info: PackageInfoWrapper): Bitmap? {
        val cached = cache.get(info.packageName)
        if (cached == null) {
            val icon = runCatching {
                Icons.loadIcon(info.source.applicationInfo)
            }.getOrNull() ?: return null
            cache.put(info.packageName, icon)
            return icon
        } else {
            return cached
        }
    }

    fun loadIconTo(
        info: PackageInfoWrapper?,
        imageView: ImageView,
        lifecycleOwner: LifecycleOwner
    ) {
        if (info == null) {
            imageView.setImageResource(R.drawable.ic_baseline_android_24)
            return
        }
        lifecycleOwner.lifecycleScope.launch {
            val icon = withContext(Dispatchers.IO) {
                loadIcon(info)
            }
            if (icon != null) {
                imageView.setImageBitmap(icon)
            } else {
                imageView.setImageResource(R.drawable.ic_baseline_android_24)
            }
        }
    }

}