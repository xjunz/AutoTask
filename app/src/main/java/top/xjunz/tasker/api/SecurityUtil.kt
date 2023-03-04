package top.xjunz.tasker.api

import io.ktor.client.request.*
import x.f

fun String.encrypt(): String {
    return f.alpha(toByteArray())
}

fun String.decrypt(): String {
    return String(f.delta(this))
}

fun HttpRequestBuilder.encryptHeaders(vararg args: String) {
    args.forEachIndexed { index, s ->
        header("$index", s.encrypt())
    }
}