/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.purchase

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.R
import top.xjunz.tasker.api.*
import top.xjunz.tasker.bridge.ClipboardManagerBridge
import top.xjunz.tasker.ktx.dec
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.service.controller.ShizukuAutomatorServiceController
import top.xjunz.tasker.service.premiumContext
import kotlin.system.exitProcess

/**
 * @author xjunz 2022/06/14
 */
class PurchaseViewModel : ViewModel() {

    private val handler = Handler(Looper.getMainLooper())

    private val client = Client()

    val refreshCountdown = MutableLiveData(0)

    val restoreCountdown = MutableLiveData(0)

    val creatingOrder = MutableLiveData<Boolean>()

    val checkingOrder = MutableLiveData<Boolean>().also { liveData ->
        liveData.observeForever {
            if (!it && currentOrder.value != null && isOrderExpired()) {
                shouldShowFeedbackPrompt.value = true
            }
        }
    }

    val checkingRedeem = MutableLiveData<Boolean>()

    val purchased = MutableLiveData<Boolean>()

    val shouldConfetti = MutableLiveData<Boolean>()

    val shouldShowFeedbackPrompt = MutableLiveData<Boolean>()

    val price = MutableLiveData<PriceDTO>()

    val orderExpirationRemainingMills = MutableLiveData(-1)

    val currentOrder = MutableLiveData<OrderDTO>()

    val shouldDismiss = MutableLiveData<Boolean>()

    init {
        loadPrice(true)
    }

    private fun loadPrice(dismissOnFailure: Boolean) = viewModelScope.launch {
        runCatching {
            client.getCurrentPrice()
        }.onFailure {
            if (dismissOnFailure) {
                shouldDismiss.value = true
            }
        }.onSuccess {
            if (it.status == HttpStatusCode.OK) {
                price.value = it.bodyAsText().decryptToDTO()
            } else if (dismissOnFailure) {
                shouldDismiss.value = true
            }
        }
    }

    fun isPurchased(): Boolean {
        return purchased.value == true
    }

    fun isOrderExpired(): Boolean {
        return orderExpirationRemainingMills.value!! <= 0
    }

    private fun MutableLiveData<Int>.startCountdown() {
        value = 5
        val countdown = object : Runnable {
            override fun run() {
                dec()
                if (require() > 0) {
                    handler.postDelayed(this, 1000L)
                }
            }
        }
        handler.postDelayed(countdown, 1000L)
    }

    private fun startOrderTimeCountdown(remainingMills: Int) {
        orderExpirationRemainingMills.postValue(remainingMills)
        val countdown = object : Runnable {

            override fun run() {
                val remaining = orderExpirationRemainingMills.value!! - 1000
                orderExpirationRemainingMills.value = remaining
                if (remaining > 0) {
                    handler.postDelayed(this, 1000L)
                }
            }
        }
        handler.postDelayed(countdown, 1000L)
    }

    fun checkRedeem(redeem: String) = viewModelScope.launch {
        checkingRedeem.value = true
        runCatching {
            client.consumeRedeem(redeem, buildDeviceId())
        }.onFailure {
            toast(R.string.format_request_failed.format(it.message))
        }.onSuccess {
            when (it.status) {
                HttpStatusCode.BadRequest -> exitProcess(-1)
                HttpStatusCode.OK -> {
                    val reply = it.bodyAsText().decryptToDTO<CodeBodyReply>()
                    when (reply.code) {
                        RedeemCode.REDEEM_EXPIRED -> toast(R.string.redeem_expired)
                        RedeemCode.REDEEM_ALREADY_CONSUMED ->
                            toast(R.string.redeem_already_consumed)
                        RedeemCode.REDEEM_NOT_FOUND -> toast(R.string.invalid_redeem)
                        RedeemCode.REDEEM_SUCCESSFULLY_CONSUMED -> {
                            if (initPremiumContext(it.bodyAsText())) {
                                val mockOrder = OrderDTO(
                                    premiumContext.orderId, -1,
                                    System.currentTimeMillis(),
                                    -1, -1, -1, ""
                                )
                                currentOrder.value = mockOrder
                            }
                        }
                        else -> toast(R.string.format_unknown_error.format(reply.code))
                    }
                }
                else -> toast(R.string.format_request_failed.format(it.status))
            }
        }
        checkingRedeem.value = false
    }

    fun createOrder() = viewModelScope.launch {
        creatingOrder.value = true
        runCatching {
            client.createOrder(buildDeviceId())
        }.onFailure {
            toast(R.string.format_request_failed.format(it.message))
        }.onSuccess {
            when (it.status) {
                HttpStatusCode.BadRequest -> exitProcess(-1)
                HttpStatusCode.TooManyRequests -> toast(R.string.too_many_requests)
                HttpStatusCode.OK -> {
                    val order = it.bodyAsText().decryptToDTO<OrderDTO>()
                    currentOrder.value = order
                    startOrderTimeCountdown(order.remainingMills)
                    if (order.originPrice != price.value?.currentValue) {
                        // The price is out of date, refresh it
                        loadPrice(false)
                    }
                }
                else -> toast(R.string.format_request_failed.format(it.status))
            }
        }
        creatingOrder.value = false
    }

    fun checkCurrentOrderState() = viewModelScope.launch {
        checkingOrder.value = true
        runCatching {
            client.checkOderState(currentOrder.require().orderId)
        }.onFailure {
            toast(R.string.format_request_failed.format(it.message))
        }.onSuccess {
            refreshCountdown.startCountdown()
            when (it.status) {
                HttpStatusCode.Forbidden -> exitProcess(-1)
                HttpStatusCode.OK -> {
                    val reply = it.bodyAsText().decryptToDTO<CodeBodyReply>()
                    when (reply.code) {
                        OrderCode.ORDER_FIRST_PAID, OrderCode.ORDER_ALREADY_PAID -> {
                            initPremiumContext(it.bodyAsText())
                            refreshCountdown.value = -1
                        }
                        OrderCode.ORDER_NOT_FOUND -> {
                            toast(R.string.order_not_found)
                        }
                        OrderCode.ORDER_FOUND_BUT_NOT_PAID -> {
                            toast(R.string.order_not_paid)
                        }
                        else -> toast(R.string.format_unknown_error.format(reply.code))
                    }
                }
                else -> toast(R.string.format_request_failed.format(it.status))
            }
        }
        checkingOrder.value = false
    }

    fun restorePurchase(orderId: String) = viewModelScope.launch {
        checkingOrder.value = true
        runCatching {
            client.restorePurchase(orderId, buildDeviceId())
        }.onFailure {
            toast(R.string.format_request_failed.format(it.message))
        }.onSuccess {
            restoreCountdown.startCountdown()
            when (it.status) {
                HttpStatusCode.BadRequest -> exitProcess(-1)
                HttpStatusCode.OK -> {
                    val reply = it.bodyAsText().decryptToDTO<CodeBodyReply>()
                    when (reply.code) {
                        PremiumUserCode.PREMIUM_USER_NEW_DEVICE,
                        PremiumUserCode.PREMIUM_USER_WITH_EXISTENT_DEVICE -> {
                            initPremiumContext(it.bodyAsText())
                            restoreCountdown.value = -1
                        }
                        PremiumUserCode.PREMIUM_USER_NOT_FOUND -> {
                            toast(R.string.order_not_paid)
                        }
                        PremiumUserCode.PREMIUM_USER_BUT_DEVICE_LIST_FULL -> {
                            toast(R.string.premium_user_device_list_full)
                        }
                        else -> toast(R.string.format_unknown_error.format(reply.code))
                    }
                }
                else -> toast(R.string.format_request_failed.format(it.status))
            }
        }
        checkingOrder.value = false
    }

    private fun initPremiumContext(raw: String): Boolean {
        var base64 = ""
        return runCatching {
            base64 = requireNotNull(raw.decryptToDTO<CodeBodyReply>().body) { "EmptyBody" }
            PremiumMixin.deserializeFromString(base64)
            // Load premium context in remote service if possible
            ShizukuAutomatorServiceController.remoteService?.loadPremiumContext()
        }.onSuccess {
            purchased.value = true
            shouldConfetti.value = true
            handler.removeCallbacksAndMessages(null)
            orderExpirationRemainingMills.value = -1
            ClipboardManagerBridge.copyToClipboard(premiumContext.orderId)
            toast(R.string.tip_order_id_copied)
            runCatching {
                PremiumMixin.storeToFile(base64)
            }.onFailure {
                it.logcatStackTrace()
            }
        }.onFailure {
            it.logcatStackTrace()
            toast(R.string.format_unknown_error.format(it.message))
        }.isSuccess
    }

    private fun buildDeviceId(): String {
        return buildString {
            append(Build.MANUFACTURER).append("|")
            append(Build.MODEL).append("|")
            append(Build.VERSION.SDK_INT).append("|")
            append(Build.BOARD).append("|")
            append(Build.BRAND).append("|")
            append(Build.HARDWARE).append("|")
            append(Build.BOOTLOADER)
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}