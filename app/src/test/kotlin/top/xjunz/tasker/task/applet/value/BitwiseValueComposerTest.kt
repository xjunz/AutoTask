/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

import org.junit.Test
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.random.Random

/**
 * @author xjunz 2023/01/08
 */
internal class BitwiseValueComposerTest {

    @Test
    fun calculateBitSize() {
        assert(ceil(log2(100F)).toInt() == 7)
        assert(ceil(log2(4F)).toInt() == 2)
        assert(ceil(log2(65535.0)).toInt() == 16)
    }

    @Test
    fun testCompose() {
        val compositeValue = BitwiseValueComposer.create(
            BitwiseValueComposer.bits(4),
            BitwiseValueComposer.integer(0xFF),
            BitwiseValueComposer.integer(0xF),
            BitwiseValueComposer.float(0xFF / 100F)
        )

        val composed = compositeValue.compose(0xF, 0x13, 0x2, 0xF1 / 100F)
        assert(composed == 0xF132F1L)
    }

    @Test
    fun parse() {
        val compositeValue = BitwiseValueComposer.create(
            BitwiseValueComposer.bits(2),
            BitwiseValueComposer.percent(),
            BitwiseValueComposer.integer(1999),
            BitwiseValueComposer.float(8.14F)
        )
        val enum = Random.nextInt(4)
        val percent = Random.nextInt(101)
        val raw = Random.nextInt(2000)
        val float = Random.nextFloat()
        val composed = compositeValue.compose(enum, percent, raw, float)
        val parsed = compositeValue.parse(composed)
        println(parsed.joinToString())
        check(parsed[0] == enum)
        check(parsed[1] == percent)
        check(parsed[2] == raw)
        check(parsed[3] == (float * BitwiseValueComposer.PRECISION_MULTIPLIER).toInt() / BitwiseValueComposer.PRECISION_MULTIPLIER)
    }

    @Test
    fun parseNullable() {
        val compositeValue = BitwiseValueComposer.create(
            BitwiseValueComposer.nullable(BitwiseValueComposer.bits(2)),
            BitwiseValueComposer.percent(),
            BitwiseValueComposer.integer(1999),
            BitwiseValueComposer.nullable(BitwiseValueComposer.float(8.14F))
        )
        val random = Random(System.currentTimeMillis())
        val enum = if (random.nextBoolean()) null else 3
        val percent = random.nextInt(101)
        val raw = random.nextInt(2000)
        val float = null
        val composed = compositeValue.compose(enum, percent, raw, float)
        val parsed = compositeValue.parse(composed)
        println(parsed.joinToString())
        assert(parsed[0] == enum)
        assert(parsed[1] == percent)
        assert(parsed[2] == raw)
        assert(parsed[3] == null)
    }
}