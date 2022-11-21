package top.xjunz.tasker.ktx

import org.intellij.lang.annotations.Language

/**
 * @author xjunz 2022/11/16
 */
fun String.firstGroupValue(@Language("RegExp") pattern: String): String? {
    return Regex(pattern).find(this)?.groupValues?.get(1)
}