/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

abstract class BaseDTO

inline fun <reified DTO : BaseDTO> DTO.encryptToText(): String {
    return Json.encodeToString(this).encrypt()
}

inline fun <reified DTO : BaseDTO> String.decryptToDTO(): DTO {
    val json = this.decrypt()
    return Json.decodeFromString(json)
}

@Serializable
data class OrderDTO(
    val orderId: String,
    val id: Int,
    val createTimestamp: Long,
    val remainingMills: Int,
    val originPrice: Int,
    val randomDiscount: Int,
    val alipayUrl: String
) : BaseDTO() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderDTO

        if (orderId != other.orderId) return false

        return true
    }

    override fun hashCode(): Int {
        return orderId.hashCode()
    }

}

@Serializable
data class PriceDTO(
    val currentValue: Int,
    val originalValue: Int?,
    val promotionText: String?,
    val startTime: String,
    val endTime: String?,
) : BaseDTO()