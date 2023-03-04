/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.api

import kotlinx.serialization.Serializable

@Serializable
class CodeBodyReply(val code: Int, val body: String? = null) : BaseDTO()

object RedeemCode {
    const val REDEEM_SUCCESSFULLY_CONSUMED = 1
    const val REDEEM_NOT_FOUND = -1
    const val REDEEM_ALREADY_CONSUMED = -2
    const val REDEEM_EXPIRED = -3
}

object OrderCode {

    const val ORDER_FOUND_BUT_NOT_PAID = 1
    const val ORDER_NOT_FOUND = -1

    /**
     * Order already referring a premium user.
     */
    const val ORDER_ALREADY_PAID = -3

    /**
     * Order is paid but no associated premium user, create a new premium user.
     */
    const val ORDER_FIRST_PAID = -4
}

object PremiumUserCode {
    const val PREMIUM_USER_WITH_EXISTENT_DEVICE = 1
    const val PREMIUM_USER_NEW_DEVICE = 2
    const val PREMIUM_USER_NOT_FOUND = -1
    const val PREMIUM_USER_BUT_DEVICE_LIST_FULL = -2
}
