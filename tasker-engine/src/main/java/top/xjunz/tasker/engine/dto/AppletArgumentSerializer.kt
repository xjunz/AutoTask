/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.dto

import kotlinx.serialization.encodeToString
import top.xjunz.shared.ktx.arrayMapOf
import top.xjunz.tasker.engine.applet.base.Applet

/**
 * @author xjunz 2023/09/22
 */
object AppletArgumentSerializer {

    @Deprecated("No longer used from version 1.1.3(16)")
    private val SEPARATOR = Char(0).toString()

    @Deprecated("No longer used from version 1.1.3(16)")
    private const val SERIALIZED_NULL_VALUE_IN_COLLECTION_PLACEHOLDER = ""

    fun Applet.valuesToStringMap(): Map<Int, String>? {
        if (values.isEmpty()) return null
        return values.mapValues {
            val type = argumentTypes[it.key]
            check(type != Applet.ARG_TYPE_REFERENCE) {
                "The arg type is reference but a value is present when serializing!"
            }
            serializeValueToStringWithType(it.key, type, it.value)
        }
    }

    private fun Applet.serializeValueToStringWithType(which: Int, argType: Int, arg: Any): String {
        return if (Applet.isCollectionArg(argType)) {
            val rawType = Applet.getRawArgType(argType)
            XTaskJson.encodeToString(
                (arg as Iterable<*>).map {
                    if (it == null) {
                        null
                    } else {
                        serializeValueToStringWithType(which, rawType, it)
                    }
                })
        } else {
            serializeArgumentToString(which, argType, arg)
        }
    }

    fun Applet.deserializeValues(raw: Map<Int, String>?) {
        if (raw == null) return
        values = raw.mapValues {
            val value = it.value
            val type = argumentTypes[it.key]
            check(type != Applet.ARG_TYPE_REFERENCE) {
                "The arg type is reference but a value is present when deserializing!"
            }
            deserializeArgumentsWithType(it.key, type, value)
        }
    }

    private fun Applet.deserializeArgumentsWithType(which: Int, argType: Int, src: String): Any {
        return if (Applet.isCollectionArg(argType)) {
            val rawType = Applet.getRawArgType(argType)
            XTaskJson.decodeFromString<List<String?>>(src).map {
                if (it == null) {
                    null
                } else {
                    deserializeArgumentFromString(which, rawType, it)
                }
            }
        } else {
            deserializeArgumentFromString(which, argType, src)
        }
    }

    fun Applet.deserializeArgumentsPreVersionCode16(src: String) {
        if (isValueInnate) return
        val valueIndex = argumentTypes.indexOfFirst { it != Applet.ARG_TYPE_REFERENCE }
        val argType = argumentTypes[valueIndex]
        val value = when {
            Applet.isCollectionArg(argType) -> {
                val split = src.split(SEPARATOR)
                val rawType = Applet.getRawArgType(argType)
                split.mapTo(ArrayList(split.size)) {
                    if (it == SERIALIZED_NULL_VALUE_IN_COLLECTION_PLACEHOLDER) null
                    else deserializeArgumentFromString(valueIndex, rawType, it)
                }
            }

            else -> deserializeArgumentFromString(valueIndex, argType, src)
        }
        values = arrayMapOf(valueIndex to value)
    }
}