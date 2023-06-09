/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.*
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.StableNodeInfo
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

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
            binding.cvBreadCrumb.setNoDoubleClickListener {
                if (nodeBreadCrumbs.isNotEmpty())
                    vm.currentNodeTree.setValueIfDistinct(nodeBreadCrumbs[adapterPosition].children[0])
            }
        }) { b, p, n ->
            b.cvBreadCrumb.text = n.shortClassName
            b.ivChevronRight.isVisible = p != nodeBreadCrumbs.lastIndex
        }
    }

    private var selectedIndex: Int = -1

    private val nodeAdapter by lazy {
        inlineAdapter(childrenNodes, ItemNodeTreeBinding::class.java, {
            binding.root.setNoDoubleClickListener {
                vm.highlightNode.value = childrenNodes[adapterPosition]
                vm.showNodeTree.value = false
                vm.makeToast(R.string.navigated_to_selected_node)
            }
            binding.btnMore.setNoDoubleClickListener {
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
            b.root.isSelected = p == selectedIndex
        }
    }

    private fun updateStableNodeInfoList(
        prev: List<StableNodeInfo>,
        cur: List<StableNodeInfo>
    ) {
        val callback = object : DiffUtil.Callback() {
            override fun getOldListSize() = prev.size

            override fun getNewListSize() = cur.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return prev[oldItemPosition] == cur[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val prevItem = prev[oldItemPosition]
                val curItem = cur[newItemPosition]

                return prevItem.name == curItem.name
                        && prevItem.source.className == curItem.source.className
                        && prevItem.caption == curItem.caption
                        && prevItem.children.size == curItem.children.size
            }

        }
        DiffUtil.calculateDiff(callback).dispatchUpdatesTo(nodeAdapter)
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
                if (!vm.highlightNode.isNull()) {
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
                binding.rvBreadCrumbs.scrollToPosition(nodeBreadCrumbs.lastIndex)
            }
            childrenNodes.clear()
            if (nodeBreadCrumbs.isNotEmpty()) {
                childrenNodes.addAll(nodeBreadCrumbs.last().children)
            }
            selectedIndex = childrenNodes.indexOfFirst { nodeInfo ->
                nodeInfo == vm.highlightNode.value || vm.highlightNode.value?.isChildOf(nodeInfo) == true
            }

            if (binding.rvNodes.adapter == null) {
                binding.rvNodes.adapter = nodeAdapter
            } else {
                nodeAdapter.notifyDataSetChanged()
                if (selectedIndex >= 0) {
                    binding.rvNodes.post {
                        binding.rvNodes.scrollPositionToCenterVertically(selectedIndex)
                    }
                }
            }
        }
        inspector.observe(vm.isCollapsed) {
            if (it) vm.currentNodeTree.value = null
        }
    }
}