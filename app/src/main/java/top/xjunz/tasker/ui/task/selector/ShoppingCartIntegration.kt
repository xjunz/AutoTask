/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Path
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.circularreveal.CircularRevealCompat
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.LayoutShoppingCartBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.SavedStateViewModel
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener
import top.xjunz.tasker.util.Motions

/**
 * @author xjunz 2022/10/08
 */
class ShoppingCartIntegration(
    private val binding: LayoutShoppingCartBinding,
    private val viewModel: SavedStateViewModel,
    private val viewToFitBottom: View
) {

    private val bottomSheet = binding.root

    private val context = binding.root.context

    private lateinit var animator: AnimatorSet

    private val circularRevealContainer = binding.circularRevealContainer

    private val ibExpand = binding.ibExpand

    private lateinit var behavior: BottomSheetBehavior<*>

    fun expand() {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun collapse() {
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun init(fragment: BaseDialogFragment<*>) {
        behavior = binding.root.requireBottomSheetBehavior()
        behavior.isHideable = false
        val key = "peekHeight"
        fragment.observe(viewModel.get<Int>(key)) {
            viewToFitBottom.updatePadding(bottom = it)
        }
        var insetTop = 0
        circularRevealContainer.applySystemInsets { container, insets ->
            insetTop = insets.top - 8.dp
            container.doOnPreDraw {
                behavior.peekHeight = it.height + insets.bottom
                viewModel.setValue(key, behavior.peekHeight)
            }
            binding.rvBottom.updatePadding(bottom = insets.bottom)
        }
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    ibExpand.setImageResource(R.drawable.ic_baseline_unfold_less_24)
                    ibExpand.setContentDescriptionAndTooltip(R.string.unfold_less.text)
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    ibExpand.setImageResource(R.drawable.ic_baseline_unfold_more_24)
                    ibExpand.setContentDescriptionAndTooltip(R.string.expand_more.text)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.rvBottom.alpha = slideOffset
                if (insetTop != 0)
                    binding.root.updatePadding(top = (slideOffset * insetTop).toInt())
            }

        })
        circularRevealContainer.setCardBackgroundColor(
            ColorUtils.setAlphaComponent(ColorScheme.colorPrimary, (0.32 * 0xFF).toInt())
        )
        ibExpand.setAntiMoneyClickListener {
            if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        fragment.dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    return@setOnKeyListener true
                } else {
                    return@setOnKeyListener fragment.onBackPressed()
                }
            }
            return@setOnKeyListener false
        }
    }

    fun animateIntoShopCart(view: View, addBackground: Boolean = true) {
        val rootView = view.rootView as ViewGroup
        val mockView = ImageView(context)
        val drawingCache = view.drawToBitmapUnsafe()
        mockView.setImageBitmap(drawingCache)
        val viewPos = IntArray(2)
        view.getLocationOnScreen(viewPos)
        val rootPos = IntArray(2)
        view.rootView.getLocationOnScreen(rootPos)
        val left = viewPos[0] - rootPos[0]
        val top = viewPos[1] - rootPos[1]
        val bezierPath = Path()
        bezierPath.moveTo(left.toFloat(), top.toFloat())
        bezierPath.quadTo(
            if (view.left + view.width / 2 < rootView.width / 2) bottomSheet.width / 2F - view.width / 2 else 0F,
            top.toFloat() - 10.dp,
            bottomSheet.width / 2F - view.width / 2,
            bottomSheet.top.toFloat()
        )
        if (addBackground)
            mockView.background = context.createMaterialShapeDrawable(cornerSize = 8.dpFloat)

        mockView.layout(left, top, left + view.width, top + view.height)
        mockView.measure(
            View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(view.height, View.MeasureSpec.EXACTLY)
        )
        rootView.overlay.add(mockView)
        mockView.animate().alpha(0F).setDuration(350).start()
        ObjectAnimator.ofFloat(mockView, View.X, View.Y, bezierPath).apply {
            interpolator = Motions.EASING_EMPHASIZED
            doOnEnd {
                rootView.overlay.remove(mockView)
            }
            doOnCancel {
                rootView.overlay.remove(mockView)
            }
        }.setDuration(350).start()
        val scalePath = Path()
        scalePath.moveTo(1F, 1F)
        scalePath.lineTo(1.25F, 1.25F)
        scalePath.close()
        animator = AnimatorSet()
        animator.play(
            CircularRevealCompat.createCircularReveal(
                circularRevealContainer,
                circularRevealContainer.width / 2F,
                circularRevealContainer.height / 2F,
                circularRevealContainer.height / 2F,
                circularRevealContainer.width / 2F
            )
        ).with(ObjectAnimator.ofFloat(circularRevealContainer, View.ALPHA, 1F, 0F))
        animator.addListener(
            CircularRevealCompat.createCircularRevealListener(circularRevealContainer)
        )
        animator.startDelay = 250
        animator.start()
    }
}