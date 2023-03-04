/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.purchase

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroupOverlay
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import top.xjunz.tasker.R
import top.xjunz.tasker.api.*
import top.xjunz.tasker.bridge.ClipboardManagerBridge
import top.xjunz.tasker.databinding.DialogPurchaseBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.main.ColorScheme
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.formatMinSec
import top.xjunz.tasker.util.formatTime
import java.util.*

/**
 * @author xjunz 2022/06/14
 */
class PurchaseDialog : BaseBottomSheetDialog<DialogPurchaseBinding>() {

    companion object {

        fun LifecycleOwner.showPurchaseDialog(toastTextResource: Int = -1) {
            if (toastTextResource != -1) {
                toast(toastTextResource)
            }
            PurchaseDialog().show(peekActivity().supportFragmentManager)
        }
    }

    private val viewModel by activityViewModels<PurchaseViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        observeLiveData()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initViews() {
        binding.priceContainer.background = MaterialShapeDrawable().apply {
            shapeAppearanceModel =
                ShapeAppearanceModel.Builder().setTopRightCornerSize(24.dpFloat)
                    .setTopLeftCornerSize(24.dpFloat).build()
            fillColor = ColorScheme.colorPrimary.toColorStateList()
        }
        binding.btnRefresh.setNoDoubleClickListener {
            viewModel.checkCurrentOrderState()
        }
        binding.btnCreateOrder.setNoDoubleClickListener {
            val order = viewModel.currentOrder.value
            if (order == null || viewModel.isOrderExpired()) {
                viewModel.createOrder()
            } else {
                val finalPrice = order.discountedPriceLiteral
                ClipboardManagerBridge.copyToClipboard(finalPrice)
                longToast(R.string.format_price.format(finalPrice))
                requireActivity().viewUrlSafely(order.alipayUrl)
            }
        }
        binding.tvRedeem.setNoDoubleClickListener {
            if (viewModel.isPurchased()) return@setNoDoubleClickListener
            TextEditorDialog().init(R.string.input_redeem.str, "") { redeem ->
                runCatching {
                    UUID.fromString(redeem)
                }.onSuccess {
                    viewModel.checkRedeem(redeem)
                }.onFailure {
                    return@init R.string.invalid_redeem.str
                }
                return@init null
            }.show(parentFragmentManager)
        }
        binding.tvOrderId.setNoDoubleClickListener {
            ClipboardManagerBridge.copyToClipboard(binding.tvOrderId.text)
            toast(R.string.order_id_copied)
        }
        binding.btnRestore.setNoDoubleClickListener {
            TextEditorDialog().init(R.string.restore_purchase.str) { orderId ->
                runCatching {
                    UUID.fromString(orderId)
                }.onSuccess {
                    viewModel.restorePurchase(orderId)
                }.onFailure {
                    toast(R.string.invalid_order)
                }
                return@init null
            }.setHint(R.string.order_id.str).show(childFragmentManager)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (viewModel.isPurchased()) {
            viewModel.currentOrder.value = null
        }
    }

    private fun String.toCNY(): String {
        return "￥$this"
    }

    private fun String.toYuan(): String {
        return R.string.format_yuan.format(this)
    }

    @SuppressLint("SetTextI18n", "SetJavaScriptEnabled")
    private fun observeLiveData() {
        viewModel.purchased.value = PremiumMixin.isPremium
        observe(viewModel.price) {
            binding.btnRefresh.isEnabled = it != null
            binding.btnCreateOrder.isEnabled = it != null
            binding.tvOriginalPrice.isVisible = it?.originalValue != null
            binding.tvPromotion.text = R.string.premium_edition_price.str
            if (it == null) {
                binding.tvPrice.text = R.string.pls_wait.str
            } else {
                binding.tvPrice.text = it.currentPriceLiteral.toCNY()
                if (it.originalValue != null) {
                    binding.tvOriginalPrice.text = it.originalPriceLiteral?.toCNY()?.strikeThrough()
                }
                if (it.promotionText != null) {
                    binding.tvPromotion.text = it.promotionText
                }
            }
        }
        observeTransient(viewModel.shouldConfetti) {
            val root = binding.root
            val overlay = root.overlay as ViewGroupOverlay
            val webView = WebView(requireContext()).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(Color.TRANSPARENT)
            }
            webView.measure(
                View.getDefaultSize(root.width, MeasureSpec.EXACTLY),
                View.getDefaultSize(root.height, MeasureSpec.EXACTLY)
            )
            webView.layout(0, 0, root.width, root.height)
            overlay.add(webView)
            webView.doOnPreDraw {
                webView.loadUrl("file:///android_asset/index-continuous.html")
            }
        }
        observeMultiple(
            viewModel.orderExpirationRemainingMills,
            viewModel.currentOrder,
            viewModel.purchased
        ) {
            val mills = viewModel.orderExpirationRemainingMills.require()
            val order = viewModel.currentOrder.value
            val isPurchased = viewModel.purchased.isTrue
            binding.tvExpiredIn.text = if (mills < 1000) {
                if (isPurchased) {
                    if (order != null && order.id < 0) {
                        R.string.redeem.str
                    } else {
                        R.string.paid.str
                    }
                } else {
                    R.string.expired.str
                }
            } else {
                R.string.format_expired_countdown.format(formatMinSec(mills))
            }
        }
        observe(viewModel.orderExpirationRemainingMills) {
            if (it < 1000) {
                binding.btnCreateOrder.text = R.string.create_order.str
            } else {
                binding.btnCreateOrder.text =
                    R.string.go_to_pay.str + viewModel.currentOrder.require().discountedPriceLiteral.toCNY()
            }
        }
        observe(viewModel.currentOrder) {
            binding.cvCurrentOrder.isVisible = it != null
            binding.cvPremium.isVisible = it == null
            if (it == null) {
                binding.btnCreateOrder.text = R.string.create_order.str
            } else {
                binding.root.rootView.beginAutoTransition()
                binding.tvOrderId.text = it.orderId.underlined()
                binding.tvCreateTime.text = it.createTimestamp.formatTime()
                if (it.originPrice == -1) {
                    binding.tvOrderPrice.text = "-"
                } else {
                    binding.tvOrderPrice.text =
                        it.discountedPriceLiteral.toYuan() + if (it.randomDiscount != 0) {
                            R.string.format_discounted.format(it.discountLiteral.toYuan())
                        } else ""
                }
            }
        }
        var progressDialog: Dialog? = null

        observe(viewModel.creatingOrder) {
            if (it) {
                progressDialog = requireActivity().makeProgressDialog()
                    .setTitle(R.string.fetching_order).show()
            } else {
                progressDialog?.dismiss()
            }
        }
        observe(viewModel.checkingOrder) {
            if (it) {
                progressDialog = requireActivity().makeProgressDialog().show()
            } else {
                progressDialog?.dismiss()
            }
        }
        observe(viewModel.refreshCountdown) {
            if (it > 0) {
                binding.btnRefresh.isEnabled = false
                binding.btnRefresh.text = R.string.format_retry_in.format(it)
            } else {
                binding.btnRefresh.isEnabled =
                    viewModel.price.value != null && viewModel.purchased.value != true
                binding.btnRefresh.text = R.string.i_have_paid.str
            }
        }
        observe(viewModel.restoreCountdown) {
            if (it > 0) {
                binding.btnRestore.isEnabled = false
                binding.btnRestore.text = R.string.format_retry_in.format(it)
            } else {
                binding.btnRestore.isEnabled = viewModel.purchased.value != true
                binding.btnRestore.text = R.string.restore_purchase.str
            }
        }
        observeTransient(viewModel.shouldDismiss) {
            dismiss()
        }
        observe(viewModel.purchased) {
            if (!it) return@observe
            binding.tvPrice.text = R.string.premium_activated.text
            binding.tvOriginalPrice.isVisible = false
            binding.tvPromotion.text = R.string.thanks_for_supporting.str
            binding.btnCreateOrder.isEnabled = false
            binding.btnRestore.isEnabled = false
            binding.btnRefresh.isVisible = false
            binding.tvCaution.isVisible = true
        }
        observe(viewModel.shouldShowFeedbackPrompt) {
            if (!it) return@observe
            /* requireContext().makeSimplePromptDialog(msg = R.string.prompt_payment_feedback) {
                 *//*Feedbacks.showPaymentFeedbackDialog(
                    requireContext(),
                    purchaseViewModel.order.value!!
                )*//*
            }.setOnDismissListener {
                purchaseViewModel.shouldShowFeedbackPrompt.value = false
            }.show()*/
        }

        var checkingRedeemDialog: AlertDialog? = null
        observe(viewModel.checkingRedeem) {
            if (it) {
                checkingRedeemDialog = requireActivity().makeProgressDialog()
                    .setTitle(R.string.checking_redeem).show()
            } else {
                checkingRedeemDialog?.dismiss()
            }
        }
    }
}
