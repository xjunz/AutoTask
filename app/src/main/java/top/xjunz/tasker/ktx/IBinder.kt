/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.os.IBinder
import android.os.IBinderHidden
import android.os.IInterface
import android.os.Parcel
import android.util.Log
import top.xjunz.shared.trace.logcat
import java.io.FileDescriptor

/**
 * @author xjunz 2022/12/24
 */
inline fun IBinder.transactOneWay(code: Int, paramSetter: (Parcel) -> Unit = {}) {
    val data = Parcel.obtain()
    val reply = Parcel.obtain()
    try {
        paramSetter(data)
        transact(code, data, reply, IBinder.FLAG_ONEWAY)
        reply.readException()
    } finally {
        data.recycle()
        reply.recycle()
    }
}

fun IBinder.execShellCmd(vararg args: String) {
    transact(IBinderHidden.SHELL_COMMAND_TRANSACTION) {
        it.writeFileDescriptor(FileDescriptor.`in`)
        it.writeFileDescriptor(FileDescriptor.out)
        it.writeFileDescriptor(FileDescriptor.err)
        it.writeStringArray(args)
    }
}

inline fun <T> IBinder.transact(
    code: Int, paramSetter: (Parcel) -> Unit = {}, resultReader: (Parcel) -> T
): T {
    val data = Parcel.obtain()
    val reply = Parcel.obtain()
    val result: T
    try {
        paramSetter(data)
        transact(code, data, reply, 0)
        reply.readException()
        result = resultReader(reply)
    } finally {
        data.recycle()
        reply.recycle()
    }
    return result
}

inline fun <T> IBinder.transactNoParams(code: Int, resultReader: (Parcel) -> T): T {
    val data = Parcel.obtain()
    val reply = Parcel.obtain()
    val result: T
    try {
        transact(code, data, reply, 0)
        reply.readException()
        result = resultReader(reply)
    } finally {
        data.recycle()
        reply.recycle()
    }
    return result
}

inline fun IBinder.transact(code: Int, paramSetter: (Parcel) -> Unit = {}) {
    val data = Parcel.obtain()
    val reply = Parcel.obtain()
    try {
        paramSetter(data)
        transact(code, data, reply, 0)
        reply.readException()
    } finally {
        data.recycle()
        reply.recycle()
    }
}

inline val IBinder.isAlive get() = isBinderAlive && pingBinder()

inline fun <I : IInterface, R> I.whenAlive(block: (I) -> R): R? {
    return if (asBinder().isAlive) {
        block(this)
    } else {
        logcat("Try to call a dead binder!", Log.WARN)
        null
    }
}