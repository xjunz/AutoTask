/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector.overlay

import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemNodeInfoBinding
import top.xjunz.tasker.databinding.OverlayNodeInfoBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener
import top.xjunz.tasker.util.Router.launchRoute
import java.util.*

/**
 * @author xjunz 2022/10/18
 */
class NodeInfoOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayNodeInfoBinding>(inspector) {

    private val uncheckedOptions = mutableSetOf<AppletOption>()

    private val uiObjectRegistry get() = AppletOptionFactory.uiObjectRegistry

    private val options = mutableListOf<AppletOption>()

    private var checkedOptions = emptyList<AppletOption>()

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.height = WindowManager.LayoutParams.MATCH_PARENT
    }

    private val adapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(options, ItemNodeInfoBinding::class.java, {
            binding.root.setAntiMoneyClickListener {
                val option = options[adapterPosition]
                if (uncheckedOptions.contains(option)) {
                    uncheckedOptions.remove(option)
                } else {
                    uncheckedOptions.add(option)
                }
                adapter.notifyItemChanged(adapterPosition, true)
            }
        }) { b, _, p ->
            b.tvAttrName.text = p.rawTitle
            b.tvAttrValue.text = p.rawDescription
            b.checkbox.isChecked = !uncheckedOptions.contains(p)
        }
    }

    private fun collectProperties() {
        val node = vm.highlightNode.require().source
        if (node.className != null)
            options.add(uiObjectRegistry.isType.withValue(node.className))

        if (node.viewIdResourceName != null)
            options.add(uiObjectRegistry.withId.withValue(node.viewIdResourceName))

        if (node.text != null)
            options.add(uiObjectRegistry.textEquals.withValue(node.text))

        if (node.contentDescription != null)
            options.add(uiObjectRegistry.contentDesc.withValue(node.contentDescription))

        if (node.isClickable)
            options.add(uiObjectRegistry.isClickable.withValue(true))

        if (node.isLongClickable)
            options.add(uiObjectRegistry.isLongClickable.withValue(true))

        if (!node.isEnabled)
            options.add(uiObjectRegistry.isEnabled.withValue(node.isEnabled))

        if (node.isCheckable)
            options.add(uiObjectRegistry.isCheckable.withValue(true))

        if (node.isChecked || node.isCheckable)
            options.add(uiObjectRegistry.isChecked.withValue(node.isChecked))

        if (node.isEditable)
            options.add(uiObjectRegistry.isEditable.withValue(true))

        options.add(uiObjectRegistry.isSelected.withValue(node.isSelected))
        if (!node.isSelected)
            uncheckedOptions.add(options.last())

        options.add(uiObjectRegistry.isScrollable.withValue(node.isScrollable))
        if (!node.isScrollable)
            uncheckedOptions.add(options.last())
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOverlayInflated() {
        super.onOverlayInflated()
        binding.btnCancel.setOnClickListener {
            vm.showNodeInfo.value = false
        }
        binding.btnComplete.setAntiMoneyClickListener {
            checkedOptions = options - uncheckedOptions
            vm.isCollapsed.value = true
            vm.showNodeInfo.value = false
            context.launchRoute(FloatingInspector.ACTION_NODE_INFO_SELECTED)
        }
        binding.container.background = context.createMaterialShapeDrawable()
        inspector.observe(vm.showNodeInfo) {
            if (!it) {
                animateHide()
                options.clear()
                uncheckedOptions.clear()
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

    fun getCheckedOptions(): List<AppletOption> {
        return ArrayList(checkedOptions).also {
            checkedOptions = emptyList()
        }
    }
}