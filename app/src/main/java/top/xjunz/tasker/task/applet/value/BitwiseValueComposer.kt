/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

import top.xjunz.shared.utils.illegalArgument
import kotlin.math.ceil
import kotlin.math.log2

/**
 * @author xjunz 2023/01/08
 */
open class BitwiseValueComposer(private var descriptors: IntArray) : ValueComposer<Number, Long>() {

    protected open val maxBitCount = Long.SIZE_BITS

    companion object {

        const val PRECISION_MULTIPLIER = 100F

        const val TYPE_RAW = 0
        const val TYPE_FLOAT = 1
        const val TYPE_PERCENT = 2

        fun create(vararg descriptors: Int): BitwiseValueComposer {
            return BitwiseValueComposer(descriptors)
        }

        private fun makeComponentDescriptor(bitCount: Int, type: Int): Int {
            return bitCount shl 4 or type
        }

        private fun isNullable(descriptor: Int): Boolean {
            return descriptor ushr 12 == 1
        }

        fun nullable(descriptor: Int): Int {
            // bit size plus one
            val newOne = makeComponentDescriptor(getBitCount(descriptor) + 1, getType(descriptor))
            return 1 shl 12 or newOne
        }

        private fun getBitCount(descriptor: Int): Int {
            return descriptor ushr 4 and 0xFF
        }

        private fun getType(descriptor: Int): Int {
            return descriptor and 0xF
        }

        private fun calculateBitSize(value: Double): Int {
            check(value > 0)
            return ceil(log2(value)).toInt()
        }

        fun boolean(): Int {
            return makeComponentDescriptor(1, TYPE_RAW)
        }

        fun integer(max: Int): Int {
            return makeComponentDescriptor(calculateBitSize(max.toDouble()), TYPE_RAW)
        }

        fun nullableInt(max: Int): Int {
            return nullable(integer(max))
        }

        fun nullableFloat(max: Float): Int {
            return nullable(float(max))
        }

        fun bits(bitCount: Int): Int {
            return makeComponentDescriptor(bitCount, TYPE_RAW)
        }

        fun float(max: Float): Int {
            return makeComponentDescriptor(
                calculateBitSize((max * PRECISION_MULTIPLIER).toDouble()), TYPE_FLOAT
            )
        }

        fun percent(): Int {
            return makeComponentDescriptor(calculateBitSize(100.0), TYPE_PERCENT)
        }
    }

    init {
        checkComponentDescriptors()
    }

    fun setComponents(vararg descriptors: Int) {
        this.descriptors = descriptors
        checkComponentDescriptors()
    }

    override fun composeInternal(components: Array<out Number?>): Long {
        check(components.size == descriptors.size) {
            "Component size and descriptor size not consistent!"
        }
        var composed = 0L
        descriptors.forEachIndexed { index, descriptor ->
            val component = components[index]
            val value: Long
            val bitCount = getBitCount(descriptor)
            if (isNullable(descriptor) && component == null) {
                value = 1L shl (bitCount - 1)
            } else {
                check(component != null)
                when (val type = getType(descriptor)) {
                    TYPE_FLOAT -> value = (component.toFloat() * PRECISION_MULTIPLIER).toLong()

                    TYPE_PERCENT -> {
                        value = component.toLong()
                        check(component in 0..100)
                    }
                    TYPE_RAW -> value = component.toLong()

                    else -> illegalArgument("type", type)
                }
            }
            check(value == 0L || calculateBitSize(value.toDouble()) <= bitCount) {
                "Value at $index out of range! Max: ${1 shl bitCount}, Value: $value"
            }
            val shift = if (index == 0) 0 else bitCount
            composed = composed shl shift or value

        }
        return composed
    }

    override fun parse(composed: Long): Array<Number?> {
        var value = composed
        var shift = 0
        val ret = arrayOfNulls<Number>(descriptors.size)
        for (i in ret.lastIndex downTo 0) {
            val descriptor = descriptors[i]
            value = value ushr shift
            val bitCount = getBitCount(descriptor)
            shift = bitCount
            if (isNullable(descriptor) && value and (1L shl bitCount - 1) != 0L) {
                // is nullable and is null
                ret[i] = null
            } else {
                val parsed = value and (1L shl bitCount) - 1
                if (getType(descriptor) == TYPE_FLOAT) {
                    ret[i] = parsed / PRECISION_MULTIPLIER
                } else {
                    ret[i] = parsed.toInt()
                }
            }
        }
        return ret
    }

    private fun checkComponentDescriptors() {
        val bitCountCum = descriptors.sumOf { getBitCount(it) }
        check(bitCountCum <= maxBitCount) {
            "Bit count overflow! Available: $maxBitCount, Required: $bitCountCum"
        }
    }
}