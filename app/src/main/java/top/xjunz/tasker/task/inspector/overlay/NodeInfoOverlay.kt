/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemNodeInfoBinding
import top.xjunz.tasker.databinding.OverlayNodeInfoBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.main.EventCenter
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import java.util.*

/**
 * @author xjunz 2022/10/18
 */
class NodeInfoOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayNodeInfoBinding>(inspector) {

    private val uncheckedApplets = mutableSetOf<Applet>()

    private val uiObjectRegistry get() = AppletOptionFactory.uiObjectRegistry

    private val allApplets = mutableListOf<Applet>()

    private var checkedApplets = emptyList<Applet>()

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.height = WindowManager.LayoutParams.MATCH_PARENT
    }

    private val adapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(allApplets, ItemNodeInfoBinding::class.java, {
            binding.root.setNoDoubleClickListener {
                val option = allApplets[adapterPosition]
                if (uncheckedApplets.contains(option)) {
                    uncheckedApplets.remove(option)
                } else {
                    uncheckedApplets.add(option)
                }
                adapter.notifyItemChanged(adapterPosition, true)
            }
        }) { b, _, applet ->
            val option = AppletOptionFactory.requireOption(applet)
            b.tvAttrName.text = option.loadSpannedTitle(applet)
            b.tvAttrValue.text = option.describe(applet)
            b.tvAttrValue.isVisible = !b.tvAttrValue.text.isNullOrEmpty()
            b.checkbox.isChecked = !uncheckedApplets.contains(applet)
        }
    }

    private fun collectProperties() {
        val node = vm.highlightNode.require().source
        if (node.className != null)
            allApplets.add(uiObjectRegistry.isType.yieldWithFirstValue(node.className))

        if (node.viewIdResourceName != null)
            allApplets.add(uiObjectRegistry.withId.yieldWithFirstValue(node.viewIdResourceName))

        if (node.text != null)
            allApplets.add(uiObjectRegistry.textEquals.yieldWithFirstValue(node.text))

        if (node.contentDescription != null)
            allApplets.add(uiObjectRegistry.contentDesc.yieldWithFirstValue(node.contentDescription))

        if (node.isClickable)
            allApplets.add(uiObjectRegistry.isClickable.yield())

        if (node.isLongClickable)
            allApplets.add(uiObjectRegistry.isLongClickable.yield())

        if (!node.isEnabled)
            allApplets.add(uiObjectRegistry.isEnabled.yieldCriterion(true))

        if (node.isCheckable)
            allApplets.add(uiObjectRegistry.isCheckable.yield())

        if (node.isChecked || node.isCheckable)
            allApplets.add(uiObjectRegistry.isChecked.yield())

        if (node.isEditable)
            allApplets.add(uiObjectRegistry.isEditable.yield())

        allApplets.add(uiObjectRegistry.isSelected.yieldCriterion(!node.isSelected))
        if (!node.isSelected)
            uncheckedApplets.add(allApplets.last())

        allApplets.add(uiObjectRegistry.isScrollable.yieldCriterion(!node.isScrollable))
        if (!node.isScrollable) {
            uncheckedApplets.add(allApplets.last())
        }

        allApplets.add(
            uiObjectRegistry.childCount.yieldWithFirstValue(
                listOf(node.childCount, node.childCount)
            )
        )
        uncheckedApplets.add(allApplets.last())
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOverlayInflated() {
        super.onOverlayInflated()
        binding.btnCancel.setOnClickListener {
            vm.showNodeInfo.value = false
        }
        binding.btnComplete.setNoDoubleClickListener {
            checkedApplets = allApplets - uncheckedApplets
            vm.isCollapsed.value = true
            vm.showNodeInfo.value = false
            EventCenter.routeEvent(
                FloatingInspector.EVENT_NODE_INFO_SELECTED, ArrayList(checkedApplets)
            )
            checkedApplets = emptyList()
        }
        binding.container.background = context.createMaterialShapeDrawable()
        inspector.observe(vm.showNodeInfo) {
            if (!it) {
                animateHide()
                allApplets.clear()
                uncheckedApplets.clear()
            } else if (vm.highlightNode.isNull()) {
                vm.makeToast(R.string.no_node_selected)
            } else {
                binding.tvTitle.text =
                    R.string.format_current.format(vm.currentMode.require().label)
                collectProperties()
                animateShow()
                if (binding.rvInfo.adapter == null) {
                    binding.rvInfo.adapter = adapter
                } else {
                    adapter.notifyDataSetChanged()
                    binding.rvInfo.scrollToPosition(0)
                }
            }
        }
    }

}