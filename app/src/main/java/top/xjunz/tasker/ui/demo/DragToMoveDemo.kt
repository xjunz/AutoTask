package top.xjunz.tasker.ui.demo

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnPreDraw
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import top.xjunz.tasker.databinding.ItemFlowOptDemoBinding
import top.xjunz.tasker.databinding.LayoutFlowOptDemoBinding
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.Motions
import java.util.*

/**
 * @author xjunz 2022/12/14
 */
class DragToMoveDemo(context: Context) : Demonstration(context) {

    private val data = Collections.nCopies(100, 0).toMutableList()

    private lateinit var animator: AnimatorSet

    private val demoAdapter =
        inlineAdapter(data, ItemFlowOptDemoBinding::class.java, {}) { _, _, _ -> }

    private val binding = LayoutFlowOptDemoBinding.inflate(LayoutInflater.from(context))

    @SuppressLint("ClickableViewAccessibility")
    override fun getView(): View {
        binding.root.doOnPreDraw {
            binding.rvDemo.adapter = demoAdapter
        }
        binding.rvDemo.setOnTouchListener { _, _ ->
            return@setOnTouchListener true
        }
        return binding.root
    }

    private var forward = true

    override fun startDemonstration() {
        val list = binding.rvDemo
        val lm = list.layoutManager as LinearLayoutManager
        val m = (lm.findLastVisibleItemPosition() + lm.findFirstVisibleItemPosition()) / 2
        val item = list.findViewHolderForAdapterPosition(m)?.itemView
        checkNotNull(item)
        val otherItem = list.findViewHolderForAdapterPosition(m - 1)?.itemView
        checkNotNull(otherItem)
        binding.pointer.translationY = 0F
        binding.pointer.y = item.y + (item.height / 2 - binding.pointer.height / 2)
        animator = AnimatorSet()
        val showPointer = ObjectAnimator.ofFloat(binding.pointer, View.ALPHA, 0F, .8F)
        val holdOn = ObjectAnimator.ofFloat(binding.pointer, View.ALPHA, .8F)
        holdOn.duration = ViewConfiguration.getLongPressTimeout().toLong()
        val hidePointer = ObjectAnimator.ofFloat(binding.pointer, View.ALPHA, 0F)
        val translationY = (item.y - otherItem.y) * if (forward) -1 else 0
        val dragItem = ObjectAnimator.ofFloat(item, View.TRANSLATION_Y, translationY)
        val movePointer = ObjectAnimator.ofFloat(
            binding.pointer,
            View.TRANSLATION_Y,
            translationY + (binding.pointer.height / 2)
        )
        val a1 = AnimatorSet()
        a1.play(holdOn).with(showPointer).before(dragItem).before(movePointer)
        val movePrevItem = ObjectAnimator.ofFloat(otherItem, View.TRANSLATION_Y, -translationY)
        movePrevItem.interpolator = Motions.EASING_EMPHASIZED
        animator.play(a1).before(hidePointer).before(movePrevItem)
        item.isPressed = true
        holdOn.doOnEnd {
            item.isSelected = true
        }
        animator.doOnEnd {
            item.isPressed = false
            item.isSelected = false
            list.postDelayed(500L) {
                startDemonstration()
            }
        }
        animator.start()
        forward = !forward
    }

    override fun stopDemonstration() {
        if (::animator.isInitialized && animator.isStarted)
            animator.cancel()
    }
}