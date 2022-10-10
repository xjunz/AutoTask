/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * @author xjunz 2022/04/20
 */

fun MutableLiveData<Int>.inc(post: Boolean = false) {
    if (post) {
        postValue((value ?: 0) + 1)
    } else {
        value = (value ?: 0) + 1
    }
}

infix fun <T> LiveData<T>.eq(value: T?): Boolean {
    return this.value == value
}

fun MutableLiveData<Int>.dec(post: Boolean = false) {
    if (post) {
        postValue((value ?: 0) - 1)
    } else {
        value = (value ?: 0) - 1
    }
}

fun <T : Any> LiveData<T>.require(): T {
    return this.value!!
}


fun MutableLiveData<Boolean>.toggle(): Boolean {
    value = value != true
    return value!!
}

fun <T : Any> MutableLiveData<T>.notifySelfChanged(post: Boolean = false) {
    if (post) {
        postValue(value)
    } else {
        value = value
    }
}

inline val LiveData<Boolean>.isTrue get() = value == true

inline val LiveData<Boolean>.isNotTrue get() = value != true
