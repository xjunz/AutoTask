/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.shared.utils

/**
 * @author xjunz 2022/07/21
 */
fun unsupportedOperation(msg: String? = null): Nothing {
    throw UnsupportedOperationException(msg ?: "Not supported")
}

fun runtimeException(msg: String): Nothing {
    throw RuntimeException(msg)
}

fun Throwable.rethrowAsRuntimeException(): Nothing {
    throw RuntimeException(this)
}

fun Throwable.rethrowInRemoteProcess(): Nothing {
    throw IllegalArgumentException(this)
}

fun illegalArgument(name: String, value: Any): Nothing {
    throw IllegalArgumentException("Unrecognized $name: $value!")
}

fun illegalArgument(msg: String? = null): Nothing {
    throw IllegalArgumentException(msg)
}
