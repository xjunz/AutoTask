/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.shared.ktx

import java.io.InputStream
import java.security.MessageDigest

/**
 * @author xjunz 2022/04/22
 */
inline val String.md5: String get() = byteInputStream().md5

inline val InputStream.md5: String
    get() {
        use {
            val md5 = MessageDigest.getInstance("md5")
            val buffer = ByteArray(10 * 1024)
            var bytes = read(buffer)
            while (bytes >= 0) {
                md5.update(buffer, 0, bytes)
                bytes = read(buffer)
            }
            return md5.digest().hexString
        }
    }

inline val ByteArray.md5: String
    get() {
        val md5 = MessageDigest.getInstance("md5")
        md5.update(this)
        return md5.digest().hexString
    }

inline val ByteArray.hexString: String
    get() {
        val lowerDigits = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        )
        val l = size
        val out = CharArray(l shl 1)
        var i = 0
        var index = 0
        while (i < l) {
            out[index++] = lowerDigits[240 and this[i].toInt() ushr 4]
            out[index++] = lowerDigits[15 and this[i].toInt()]
            ++i
        }
        return String(out)
    }