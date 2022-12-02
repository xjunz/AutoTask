/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker

import android.content.SharedPreferences
import androidx.core.content.edit
import top.xjunz.tasker.service.OperatingMode
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType

/**
 * @author xjunz 2022/04/21
 */
object Configurations {

    private val global by lazy {
        app.sharedPrefsOf("global")
    }

    var operatingMode by global.primitive("operating_mode", OperatingMode.Shizuku.VALUE)

    var showMultiReferenceHelp by global.primitive("show_multi_reference_help", true)

    private fun <T> SharedPreferences.nullable(
        name: String,
        defValue: T?
    ): NullableConfiguration<T> {
        return NullableConfiguration(this, name, defValue)
    }

    private fun <T> SharedPreferences.primitive(
        name: String,
        defValue: T
    ): PrimitiveConfiguration<T> {
        return PrimitiveConfiguration(this, name, defValue)
    }

    @Suppress("UNCHECKED_CAST")
    class PrimitiveConfiguration<T>(
        private val sp: SharedPreferences,
        private val key: String,
        private val defValue: T
    ) : ReadWriteProperty<Any, T> {
        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return when (val type = property.returnType.javaType) {
                Boolean::class.java -> sp.getBoolean(key, defValue as Boolean) as T
                Int::class.java -> sp.getInt(key, defValue as Int) as T
                Long::class.java -> sp.getLong(key, defValue as Long) as T
                Float::class.java -> sp.getFloat(key, defValue as Float) as T
                else -> error("unsupported type: $type")
            }
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            sp.edit {
                when (val type = property.returnType.javaType) {
                    Boolean::class.java -> putBoolean(key, value as Boolean)
                    Int::class.java -> putInt(key, value as Int)
                    Long::class.java -> putLong(key, value as Long)
                    Float::class.java -> putFloat(key, value as Float)
                    else -> error("unsupported type: $type")
                }
            }
        }

    }

    @Suppress("UNCHECKED_CAST")
    class NullableConfiguration<T : Any?>(
        private val sp: SharedPreferences,
        private val key: String,
        private val defValue: T?
    ) : ReadWriteProperty<Any, T?> {

        override fun getValue(thisRef: Any, property: KProperty<*>): T? {
            return when (val type = property.returnType.javaType) {
                String::class.java -> sp.getString(key, defValue as? String) as T
                Set::class.java -> sp.getStringSet(
                    key, defValue as? Set<String>
                ) as T
                else -> error("unsupported type: $type")
            }
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
            sp.edit {
                when (val type = property.returnType.javaType) {
                    String::class.java -> putString(key, value as? String)
                    Set::class.java -> putStringSet(key, value as? Set<String>)
                    else -> error("unsupported type: $type")
                }
            }
        }
    }
}