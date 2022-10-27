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

fun LiveData<*>.isNull(): Boolean {
    return value == null
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

/**
 * Reassign the value to itself, useful when the value is a instance reference and you want to
 * notify its changes.
 */
fun <T : Any> MutableLiveData<T>.notifySelfChanged(post: Boolean = false) {
    if (post) {
        postValue(value)
    } else {
        value = value
    }
}

/**
 * Only set value when the livedata has active observers, useful for non-sticky scenarios.
 */
fun <T> MutableLiveData<T>.setValueIfObserved(newValue: T?, post: Boolean = false) {
    if (!hasActiveObservers()) return
    if (post) {
        postValue(newValue)
    } else {
        value = newValue
    }
}

/**
 * Only set value when new value does not equal to the old value.
 */
fun <T> MutableLiveData<T>.setValueIfDistinct(newValue: T?, post: Boolean = false) {
    if (value == newValue) return
    if (post) {
        postValue(newValue)
    } else {
        value = newValue
    }
}

inline val LiveData<Boolean>.isTrue get() = value == true

inline val LiveData<Boolean>.isNotTrue get() = value != true
