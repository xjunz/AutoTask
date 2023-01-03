/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.shared.utils

import android.system.Os


/**
 * @author xjunz 2023/01/03
 */
object OsUtil {

    val uid: Int

    val aid: Int

    inline val isInShellProcess get() = uid == 2000

    inline val isInRootProcess get() = uid == 0

    init {
        val i = Os.getuid()
        uid = i
        aid = i / 100_000
    }
}