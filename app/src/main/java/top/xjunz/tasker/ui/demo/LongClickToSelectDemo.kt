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
import java.util.*

/**
 * @author xjunz 2022/12/13
 */
class LongClickToSelectDemo(context: Context) : Demonstration(context) {

    private val data = Collections.nCopies(100, 0).toMutableList()

    private var selectedItem: Int = -1

    private lateinit var animator: AnimatorSet

    private val demoAdapter by lazy {
        inlineAdapter(data, ItemFlowOptDemoBinding::class.java, {}) { b, p, _ ->
            b.root.isSelected = selectedItem == p
        }
    }

    private val binding by lazy {
        LayoutFlowOptDemoBinding.inflate(LayoutInflater.from(context))
    }

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

    override fun startDemonstration() {
        val list = binding.rvDemo
        val lm = list.layoutManager as LinearLayoutManager
        val m = (lm.findLastVisibleItemPosition() + lm.findFirstVisibleItemPosition()) / 2
        val item = list.findViewHolderForAdapterPosition(m)?.itemView
        checkNotNull(item)

        binding.pointer.y = item.y + item.height / 2 - binding.pointer.height / 2
        animator = AnimatorSet()
        val showPointer = ObjectAnimator.ofFloat(binding.pointer, View.ALPHA, 0F, .8F)
        val holdOn = ObjectAnimator.ofFloat(binding.pointer, View.ALPHA, .8F)
        holdOn.duration = ViewConfiguration.getLongPressTimeout().toLong()
        val hidePointer = ObjectAnimator.ofFloat(binding.pointer, View.ALPHA, 0F)

        holdOn.doOnEnd {
            selectedItem = m
            demoAdapter.notifyItemChanged(m, true)
            item.isPressed = false
            list.postDelayed(1000L) {
                selectedItem = -1
                demoAdapter.notifyItemChanged(m, true)
            }
            list.postDelayed(2000L) {
                startDemonstration()
            }
        }
        animator.play(holdOn).before(hidePointer).after(showPointer)
        animator.start()
        item.isPressed = true
    }

    override fun stopDemonstration() {
        if (::animator.isInitialized && animator.isStarted)
            animator.cancel()
    }
}