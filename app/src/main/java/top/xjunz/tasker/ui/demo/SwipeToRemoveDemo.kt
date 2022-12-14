package top.xjunz.tasker.ui.demo

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnPreDraw
import androidx.core.view.postDelayed
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import top.xjunz.tasker.databinding.ItemFlowOptDemoBinding
import top.xjunz.tasker.databinding.LayoutFlowOptDemoBinding
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.Motions
import java.util.*

/**
 * @author xjunz 2022/12/12
 */
class SwipeToRemoveDemo(context: Context) : Demonstration(context) {

    private val data = Collections.nCopies(10, 0).toMutableList()

    private lateinit var animator: AnimatorSet

    private val demoAdapter =
        inlineAdapter(data, ItemFlowOptDemoBinding::class.java, {}) { b, _, _ ->
            b.root.translationX = 0F
        }

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

    private var multiplier = 1

    override fun startDemonstration() {
        val list = binding.rvDemo
        val lm = list.layoutManager as LinearLayoutManager
        val m = (lm.findLastVisibleItemPosition() + lm.findFirstVisibleItemPosition()) / 2
        val item = list.findViewHolderForAdapterPosition(m)?.itemView
        checkNotNull(item)
        binding.pointer.y = item.y + item.height / 2 - binding.pointer.height / 2
        binding.pointer.translationX = 0F
        animator = AnimatorSet()
        val showPointer = ObjectAnimator.ofFloat(binding.pointer, View.ALPHA, 0F, .8F)
        val swipePointer = ObjectAnimator.ofFloat(
            binding.pointer, View.TRANSLATION_X, 0F, multiplier * item.width / 2F
        )
        swipePointer.interpolator = FastOutSlowInInterpolator()
        swipePointer.duration = 500
        val swipeItem = ObjectAnimator.ofFloat(
            item, View.TRANSLATION_X, 0F,
            multiplier * item.width.toFloat()
        )
        swipeItem.interpolator = Motions.EASING_EMPHASIZED
        swipeItem.duration = 530
        val hidePointer = ObjectAnimator.ofFloat(binding.pointer, View.ALPHA, 0F)
        swipeItem.doOnEnd {
            data.removeAt(m)
            demoAdapter.notifyItemRemoved(m)
            data.add(0)
            multiplier = -multiplier
            list.postDelayed(800L) {
                item.translationX = 0F
                startDemonstration()
            }
        }
        animator.play(swipePointer).with(swipeItem).after(showPointer).with(hidePointer)
        animator.start()
    }

    override fun stopDemonstration() {
        if (::animator.isInitialized && animator.isStarted)
            animator.cancel()
    }
}