/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.shared.ktx

import androidx.collection.ArrayMap

/**
 * @author xjunz 2023/10/11
 */
fun <K, V> arrayMapOf(vararg entries: Pair<K, V>): ArrayMap<K, V> {
    val map = ArrayMap<K, V>(entries.size)
    map.putAll(entries)
    return map
}