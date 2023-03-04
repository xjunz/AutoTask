/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.api

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * @author xjunz 2022/06/15
 */
private fun toLiteralPrice(amount: Int): String {
    val ret = BigDecimal(amount).divide(BigDecimal(100), 2, RoundingMode.UNNECESSARY)
    return ret.toString()
}

val OrderDTO.discountedPrice: Int get() = originPrice - randomDiscount

val OrderDTO.discountedPriceLiteral: String get() = toLiteralPrice(discountedPrice)

val OrderDTO.discountLiteral: String get() = toLiteralPrice(randomDiscount)

val PriceDTO.currentPriceLiteral: String get() = toLiteralPrice(currentValue)

val PriceDTO.originalPriceLiteral: String?
    get() = if (originalValue == null) null else toLiteralPrice(
        originalValue
    )