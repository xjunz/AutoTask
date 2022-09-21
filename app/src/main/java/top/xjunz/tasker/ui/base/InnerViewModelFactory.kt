/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * An [AbstractSavedStateViewModelFactory] which supports initiating an inner-class [ViewModel].
 *
 * @author xjunz 2022/05/08
 */
object InnerViewModelFactory : AbstractSavedStateViewModelFactory() {

    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        runCatching {
            val constructor = modelClass.getDeclaredConstructor(SavedStateHandle::class.java)
            constructor.isAccessible = true
            return constructor.newInstance(handle)
        }.onFailure {
            val constructor = modelClass.getDeclaredConstructor()
            constructor.isAccessible = true
            return constructor.newInstance()
        }
        throw RuntimeException("No suitable constructor!")
    }
}
