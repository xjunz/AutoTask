/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.premium

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import top.xjunz.tasker.upForGrabs
import x.f
import java.io.ByteArrayInputStream
import java.io.File
import java.lang.ref.WeakReference
import java.util.zip.CRC32

/**
 * @author xjunz 2023/03/01
 */

object PremiumMixin {

    lateinit var premiumContextStoragePath: String

    private var PREMIUM_CONTEXT: PremiumContext? = null
        set(value) {
            field = value
            _premiumStatusLiveData.postValue(field != null)
            callbacks.forEach {
                it.get()?.onPremiumStateChanged(field != null)
            }
        }

    const val PREMIUM_CONTEXT_FILE_NAME = ".lock"

    val premiumStatusLiveData: LiveData<Boolean> get() = _premiumStatusLiveData

    private val _premiumStatusLiveData = MutableLiveData<Boolean>()

    private val callbacks = arrayListOf<WeakReference<Callback>>()

    fun interface Callback {
        fun onPremiumStateChanged(isPremium: Boolean)
    }

    fun addOnPremiumStateChangedCallback(callback: Callback) {
        callbacks.add(WeakReference(callback))
    }

    fun ensurePremium() {
        if (PREMIUM_CONTEXT == null) {
            throw PaymentRequiredException()
        }
    }

    val premiumContext: PremiumContext
        get() {
            ensurePremium()
            return PREMIUM_CONTEXT!!
        }

    fun storeToFile(base64: String): Boolean {
        val file = File(premiumContextStoragePath)
        var prepared = false
        if (file.exists()) {
            prepared = true
        } else {
            val parent = file.parentFile
            if (parent != null && (parent.exists() || parent.mkdirs())) {
                if (file.createNewFile()) {
                    prepared = true
                }
            }
        }
        if (prepared) {
            file.outputStream().bufferedWriter().use {
                it.write(base64)
            }
            return true
        }
        return false
    }

    fun loadPremiumFromFileSafely() {
        if (upForGrabs) {
            PREMIUM_CONTEXT = PremiumContext()
        } else {
            val file = File(premiumContextStoragePath)
            if (file.exists()) {
                runCatching {
                    deserializeFromString(file.readText())
                }.onFailure {
                    file.delete()
                }
            }
        }
    }

    fun clearPremium() {
        File(premiumContextStoragePath).delete()
        PREMIUM_CONTEXT = null
    }

    fun deserializeFromString(base64: String) {
        val values = arrayListOf<String>()
        val decrypted = f.delta(base64)
        ByteArrayInputStream(decrypted).bufferedReader().useLines { lines ->
            values.addAll(lines.toList())
        }
        val ctx = PremiumContext()
        val fields = PremiumContext::class.java.declaredFields.filter {
            it.getAnnotation(FieldOrdinal::class.java) != null
        }.sortedBy {
            it.getAnnotation(FieldOrdinal::class.java)?.ordinal
        }
        check(fields.size == values.size) { "check size failed" }
        val crc = CRC32()
        fields.forEachIndexed { index, field ->
            field.isAccessible = true
            val value = values[index]
            field.set(ctx, value)
            if (index < fields.lastIndex) {
                crc.update(value.toByteArray())
            }
        }
        if (crc.value.toString() != ctx.checksum) {
            throw RuntimeException("check sum failed")
        }
        PREMIUM_CONTEXT = ctx
    }
}