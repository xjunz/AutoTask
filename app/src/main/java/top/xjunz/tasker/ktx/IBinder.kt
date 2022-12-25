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
    transactOneWay(IBinderHidden.SHELL_COMMAND_TRANSACTION) {
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

inline fun <I : IInterface> I.whenAlive(block: (I) -> Unit) {
    val binder = asBinder()
    if (binder.isBinderAlive && binder.pingBinder()) {
        block(this)
    } else {
        logcat("Try to call a dead binder!", Log.WARN)
    }
}