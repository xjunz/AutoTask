package top.xjunz.tasker.task.inspector.overlay

import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.Rect
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.DisplayManagerBridge
import top.xjunz.tasker.databinding.ItemNodeInfoBinding
import top.xjunz.tasker.databinding.OverlayNodeInfoBinding
import top.xjunz.tasker.engine.value.Distance
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.registry.FlowOptionRegistry
import top.xjunz.tasker.task.applet.option.registry.PackageOptionRegistry
import top.xjunz.tasker.task.applet.option.registry.UiObjectOptionRegistry
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener
import top.xjunz.tasker.util.Router
import top.xjunz.tasker.util.Router.launchRoute
import java.util.*

/**
 * @author xjunz 2022/10/18
 */
class NodeInfoOverlay(inspector: FloatingInspector) :
    FloatingInspectorOverlay<OverlayNodeInfoBinding>(inspector) {

    private val uncheckedOptions = mutableSetOf<AppletOption>()

    private val uiObjectFactory =
        UiObjectOptionRegistry(FlowOptionRegistry.ID_UI_OBJECT_OPTION_REGISTRY)

    private val pkgFactory = PackageOptionRegistry(FlowOptionRegistry.ID_PKG_OPTION_REGISTRY)

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
        if (vm.currentMode eq InspectorMode.COMPONENT || vm.showExtraOptions) {
            vm.currentComp.value?.let {
                options.add(pkgFactory.pkgCollection.withValue(Collections.singleton(it.pkgName)))

                if (it.actName != null) {
                    val compName = ComponentName(it.pkgName, it.actName!!).flattenToShortString()
                    options.add(
                        pkgFactory.activityCollection.withValue(Collections.singleton(compName))
                    )
                }

                if (it.paneTitle != null)
                    options.add(pkgFactory.paneTitle.withValue(it.paneTitle))
            }
        }
        if (vm.currentMode eq InspectorMode.COMPONENT) return

        val node = vm.emphaticNode.require().source
        if (node.className != null)
            options.add(uiObjectFactory.isType.withValue(node.className))

        if (node.viewIdResourceName != null)
            options.add(uiObjectFactory.withId.withValue(node.viewIdResourceName))

        if (node.text != null)
            options.add(uiObjectFactory.textEquals.withValue(node.text))

        if (node.contentDescription != null)
            options.add(uiObjectFactory.contentDesc.withValue(node.contentDescription))

        if (node.isClickable)
            options.add(uiObjectFactory.isClickable.withValue(true))

        if (node.isLongClickable)
            options.add(uiObjectFactory.isLongClickable.withValue(true))

        if (!node.isEnabled)
            options.add(uiObjectFactory.isEnabled.withValue(node.isEnabled))

        if (node.isCheckable)
            options.add(uiObjectFactory.isCheckable.withValue(true))

        if (node.isChecked || node.isCheckable)
            options.add(uiObjectFactory.isChecked.withValue(node.isChecked))

        if (node.isEditable)
            options.add(uiObjectFactory.isEditable.withValue(true))

        options.add(uiObjectFactory.isSelected.withValue(node.isSelected))
        if (!node.isSelected)
            uncheckedOptions.add(options.last())

        options.add(uiObjectFactory.isScrollable.withValue(node.isScrollable))
        if (!node.isScrollable)
            uncheckedOptions.add(options.last())

        val rScreen = Rect()
        node.getBoundsInScreen(rScreen)

        options.add(uiObjectFactory.left.withValue(Distance.exactPxInScreen(rScreen.left)))

        options.add(uiObjectFactory.right.withValue(Distance.exactPxInScreen(DisplayManagerBridge.size.x - rScreen.right)))

        options.add(uiObjectFactory.top.withValue(Distance.exactPxInScreen(rScreen.top)))

        options.add(uiObjectFactory.bottom.withValue(Distance.exactPxInScreen(DisplayManagerBridge.size.y - rScreen.bottom)))

        options.add(uiObjectFactory.width.withValue(Distance.exactPx(rScreen.width())))
        options.add(uiObjectFactory.height.withValue(Distance.exactPx(rScreen.height())))
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
            context.launchRoute(Router.HOST_ACCEPT_OPTIONS_FROM_INSPECTOR)
        }
        binding.container.background = context.createMaterialShapeDrawable()
        inspector.observe(vm.showNodeInfo) {
            if (!it) {
                animateHide()
                options.clear()
                uncheckedOptions.clear()
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