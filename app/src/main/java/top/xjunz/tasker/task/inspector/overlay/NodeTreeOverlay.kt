/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.annotation.SuppressLint
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.*
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.StableNodeInfo
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/10/18
 */
@SuppressLint("SetTextI18n")
class NodeTreeOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayNodeTreeBinding>(inspector) {

    private val nodeBreadCrumbs = mutableListOf<StableNodeInfo>()

    private val childrenNodes = mutableListOf<StableNodeInfo>()

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.flags = base.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.height = WindowManager.LayoutParams.MATCH_PARENT
    }

    private val breadCrumbAdapter by lazy {
        inlineAdapter(nodeBreadCrumbs, ItemBreadCrumbsBinding::class.java, {
            binding.cvBreadCrumb.setAntiMoneyClickListener {
                if (nodeBreadCrumbs.isNotEmpty())
                    vm.currentNodeTree.setValueIfDistinct(nodeBreadCrumbs[adapterPosition].children[0])
            }
        }) { b, p, n ->
            b.tvTitle.text = n.shortClassName
            b.ivChevronRight.isVisible = p != nodeBreadCrumbs.lastIndex
        }
    }

    private val nodeAdapter by lazy {
        inlineAdapter(childrenNodes, ItemNodeTreeBinding::class.java, {
            binding.root.setAntiMoneyClickListener {
                vm.highlightNode.value = childrenNodes[adapterPosition]
                vm.showNodeTree.value = false
                vm.makeToast(R.string.navigated_to_selected_node)
            }
            binding.btnMore.setAntiMoneyClickListener {
                vm.currentNodeTree.value = childrenNodes[adapterPosition].children[0]
            }
        }) { b, p, n ->
            b.tvOrdinal.text = (p + 1).toString()
            b.tvTitle.text = n.caption
            b.tvBadge.isVisible = n.name != null
            b.tvBadge.text = n.name
            b.tvDesc.text = n.source.className
            b.btnMore.isVisible = n.children.isNotEmpty()
            b.btnMore.text = n.children.size.toString()
            b.root.isSelected =
                n == vm.highlightNode.value || vm.highlightNode.value?.isChildOf(n) == true
            if (!vm.shouldAnimateItems) return@inlineAdapter
            val staggerAnimOffsetMills = 30L
            val easeIn = AnimationUtils.loadAnimation(context, R.anim.mtrl_item_ease_enter)
            easeIn.startOffset = (staggerAnimOffsetMills + p) * p
            b.root.startAnimation(easeIn)
            if (p != 0) return@inlineAdapter
            inspector.lifecycleScope.launch {
                delay(staggerAnimOffsetMills)
                vm.shouldAnimateItems = false
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOverlayInflated() {
        super.onOverlayInflated()
        binding.container.background = context.createMaterialShapeDrawable()
        binding.ibClose.setOnClickListener {
            vm.showNodeTree.value = false
        }
        inspector.observe(vm.showNodeTree) {
            if (rootView.isVisible == it) return@observe
            if (it) {
                if (vm.highlightNode.value != null) {
                    vm.currentNodeTree.value = vm.highlightNode.value
                    vm.makeToast(R.string.navigated_to_selected_node)
                }
                animateShow()
            } else {
                animateHide()
            }
        }
        inspector.observe(vm.currentNodeTree) {
            nodeBreadCrumbs.clear()
            var parent = it?.parent
            if (it != null && parent == null) {
                nodeBreadCrumbs.add(it)
            }
            while (parent != null) {
                nodeBreadCrumbs.add(0, parent)
                parent = parent.parent
            }
            if (binding.rvBreadCrumbs.adapter == null) {
                binding.rvBreadCrumbs.adapter = breadCrumbAdapter
            } else {
                breadCrumbAdapter.notifyDataSetChanged()
                if (nodeBreadCrumbs.isNotEmpty()) {
                    if (rootView.isVisible) {
                        binding.rvBreadCrumbs.smoothScrollToPosition(nodeBreadCrumbs.lastIndex)
                    } else {
                        binding.rvBreadCrumbs.scrollToPosition(nodeBreadCrumbs.lastIndex)
                    }
                }
            }
            childrenNodes.clear()
            if (nodeBreadCrumbs.isNotEmpty()) {
                childrenNodes.addAll(nodeBreadCrumbs.last().children)
            }
            if (binding.rvNodes.adapter == null) {
                binding.rvNodes.adapter = nodeAdapter
            } else {
                vm.shouldAnimateItems = true
                nodeAdapter.notifyDataSetChanged()
                if (childrenNodes.isNotEmpty()) {
                    binding.rvNodes.smoothScrollToPosition(0)
                }
            }
        }
        inspector.observe(vm.isCollapsed) {
            if (it) vm.currentNodeTree.value = null
        }
    }
}