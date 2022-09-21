package top.xjunz.tasker.util

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

fun illegalArgument(name: String, value: Any): Nothing {
    throw IllegalArgumentException("Unrecognized $name: $value!")
}

fun illegalArgument(msg: String): Nothing {
    throw IllegalArgumentException(msg)
}
