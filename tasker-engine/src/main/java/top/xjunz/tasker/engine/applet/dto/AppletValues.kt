/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.dto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.value.Distance

/**
 * A helper class for serializing and deserializing an [Applet]'s value. Note there is no `boolean`
 * type of value, because a `boolean` value can be controlled over [Applet.isInverted].
 *
 * @author xjunz 2022/08/14
 */
object AppletValues {

    const val VAL_TYPE_IRRELEVANT = 0
    const val VAL_TYPE_TEXT = 1

    @Deprecated("Unsupported! Use [Applet.isInverted] to control boolean value.")
    const val VAL_TYPE_BOOL = VAL_TYPE_IRRELEVANT
    const val VAL_TYPE_INT = 3
    const val VAL_TYPE_FLOAT = 4
    const val VAL_TYPE_LONG = 5

    /**
     * @see Distance
     */
    const val VAL_TYPE_DISTANCE = 6


    /**
     * Bit mask for collection value type.
     */
    internal const val MASK_VAL_TYPE_COLLECTION = 1 shl 8

    private val SEPARATOR = Char(0).toString()

    /**
     * Whether its value is a [Collection].
     */
    private val Applet.isCollectionValue: Boolean
        get() = valueType and MASK_VAL_TYPE_COLLECTION != 0

    /**
     * Unmasked raw type.
     *
     * @see MASK_VAL_TYPE_COLLECTION
     * @see Applet.valueType
     */
    val Applet.rawType: Int
        get() = valueType and MASK_VAL_TYPE_COLLECTION.inv()

    inline fun <reified T> judgeValueType(): Int {
        return when (val clz = T::class.java) {
            Int::class.java, Int::class.javaObjectType -> VAL_TYPE_INT
            String::class.java -> VAL_TYPE_TEXT
            Float::class.java, Float::class.javaObjectType -> VAL_TYPE_FLOAT
            Long::class.java, Long::class.javaObjectType -> VAL_TYPE_LONG
            Distance::class.java -> VAL_TYPE_DISTANCE
            Unit::class.java -> VAL_TYPE_IRRELEVANT
            else -> illegalArgument("type", clz.name)
        }
    }

    private fun Applet.judgeType(value: Any, knownCollection: Boolean = false): Int {
        return when (value) {
            is CharSequence -> VAL_TYPE_TEXT
            is Int -> VAL_TYPE_INT
            is Long -> VAL_TYPE_LONG
            is Float -> VAL_TYPE_FLOAT
            is Distance -> VAL_TYPE_DISTANCE
            is Collection<*> -> {
                if (knownCollection) illegalArgument("Nested collection is not supported!")
                if (value.size == 0) illegalArgument("Empty collection is not supported!")
                judgeType(value.first()!!, true) or MASK_VAL_TYPE_COLLECTION
            }
            else -> {
                if (knownCollection) illegalArgument("Unsupported collection type: ${value::class.java.name}!")
                illegalArgument("Unsupported type: ${value::class.java.name}!")
            }
        }
    }

    private fun serialize(rawType: Int, value: Any): String {
        return if (rawType == VAL_TYPE_DISTANCE) {
            value as Distance
            Json.encodeToString(value)
        } else {
            value.toString()
        }
    }

    internal fun Applet.serializeValue(value: Any?): String? {
        return when {
            value == null -> null
            rawType == VAL_TYPE_IRRELEVANT -> null
            isCollectionValue -> {
                value as Collection<*>
                value.joinToString(SEPARATOR) {
                    serialize(rawType, it!!)
                }
            }
            else -> serialize(rawType, value)
        }
    }

    private fun deserialize(rawType: Int, src: String): Any? {
        return when (rawType) {
            VAL_TYPE_IRRELEVANT -> null
            VAL_TYPE_TEXT -> src
            VAL_TYPE_FLOAT -> src.toFloat()
            VAL_TYPE_INT -> src.toInt()
            VAL_TYPE_LONG -> src.toLong()
            VAL_TYPE_DISTANCE -> Json.decodeFromString<Distance>(src)
            else -> illegalArgument("value type", rawType)
        }
    }

    internal fun Applet.deserializeValue(src: String?): Any? {
        if (src == null) return null
        return if (isCollectionValue) {
            val split = src.split(SEPARATOR)
            split.mapTo(ArrayList(split.size)) {
                deserialize(rawType, it)
            }
        } else {
            deserialize(rawType, src)
        }
    }

}