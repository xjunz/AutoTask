/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipOutputStream

/**
 * @author xjunz 2022/05/15
 */
fun ZipOutputStream.compress(file: File, name: String) {
    compress(file.inputStream(), name)
}

fun ZipOutputStream.compress(text: String, name: String) {
    if (!putNextEntryMayDuplicate(ZipEntry(name))) return
    write(text.toByteArray())
    closeEntry()
}

fun ZipOutputStream.compress(input: InputStream, name: String) {
    if (!putNextEntryMayDuplicate(ZipEntry(name))) return
    input.use {
        it.copyTo(this)
    }
    closeEntry()
}

fun ZipOutputStream.putNextEntryMayDuplicate(zipEntry: ZipEntry): Boolean {
    try {
        putNextEntry(zipEntry)
    } catch (e: ZipException) {
        if (e.message?.startsWith("duplicate entry") == true) {
            return false
        }
        throw e
    }
    return true
}