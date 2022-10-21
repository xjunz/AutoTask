package top.xjunz.tasker.engine

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.xjunz.tasker.engine.base.Applet
import top.xjunz.tasker.engine.valt.Distance
import top.xjunz.tasker.util.illegalArgument

/**
 * A helper class for serialize and deserialize an [Applet]'s value.
 *
 * @author xjunz 2022/08/14
 */
object AppletValueSerializer {

    private const val VAL_TYPE_TEXT = 1
    private const val VAL_TYPE_BOOLEAN = 2
    private const val VAL_TYPE_INT = 3
    private const val VAL_TYPE_FLOAT = 4

    /**
     * @see Distance
     */
    private const val VAL_TYPE_DISTANCE = 1 shl 4

    /**
     * Bit mask for collection value type.
     */
    private const val MASK_VAL_TYPE_COLLECTION = 1 shl 8

    private val SEPARATOR = Char(0).toString()

    /**
     * Whether its value is a [Collection].
     */
    private inline val Applet.isValueCollection: Boolean
        get() = valueType and MASK_VAL_TYPE_COLLECTION != 0

    /**
     * Unmasked raw type.
     *
     * @see MASK_VAL_TYPE_COLLECTION
     */
    private inline val Applet.rawType: Int
        get() = valueType and MASK_VAL_TYPE_COLLECTION.inv()

    fun Applet.judgeValueType(value: Any, knownCollection: Boolean = false): Int {
        return when (value) {
            is CharSequence -> VAL_TYPE_TEXT
            is Boolean -> VAL_TYPE_BOOLEAN
            is Int -> VAL_TYPE_INT
            is Float -> VAL_TYPE_FLOAT
            is Distance -> VAL_TYPE_DISTANCE
            is Collection<*> -> {
                if (knownCollection) illegalArgument("Nested collection is not supported!")
                if (value.size == 0) illegalArgument("Empty collection is not supported!")
                judgeValueType(value.first()!!, true) or MASK_VAL_TYPE_COLLECTION
            }
            else -> {
                if (knownCollection) illegalArgument("Unsupported collection type: ${value::class.java.name}!")
                illegalArgument("Unsupported type: ${value::class.java.name}!")
            }
        }
    }

    private fun serializeValue(rawType: Int, value: Any): String {
        return if (rawType == VAL_TYPE_DISTANCE) {
            value as Distance
            Json.encodeToString(value)
        } else {
            value.toString()
        }
    }

    fun Applet.serialize(value: Any): String {
        return if (isValueCollection) {
            value as Collection<*>
            value.joinToString(SEPARATOR) {
                serializeValue(rawType, value)
            }
        } else {
            serializeValue(rawType, value)
        }
    }

    private fun deserializeValue(rawType: Int, src: String): Any {
        return when (rawType) {
            VAL_TYPE_TEXT -> src
            VAL_TYPE_BOOLEAN -> src.toBoolean()
            VAL_TYPE_FLOAT -> src.toFloat()
            VAL_TYPE_INT -> src.toInt()
            VAL_TYPE_DISTANCE -> Json.decodeFromString<Distance>(src)
            else -> illegalArgument("value type", rawType)
        }
    }

    fun Applet.deserialize(src: String): Any {
        return if (isValueCollection) {
            val split = src.split(SEPARATOR)
            split.map {
                deserializeValue(rawType, src)
            }
        } else {
            deserializeValue(rawType, src)
        }
    }

}