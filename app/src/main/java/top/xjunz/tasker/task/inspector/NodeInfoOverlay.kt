package top.xjunz.tasker.task.inspector

import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.Rect
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemNodeInfoBinding
import top.xjunz.tasker.databinding.OverlayNodeInfoBinding
import top.xjunz.tasker.engine.valt.Distance
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.factory.AppletOption
import top.xjunz.tasker.task.factory.FlowFactory
import top.xjunz.tasker.task.factory.PackageAppletFactory
import top.xjunz.tasker.task.factory.UiObjectAppletFactory
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.Router
import top.xjunz.tasker.util.Router.routeTo
import java.util.*

/**
 * @author xjunz 2022/10/18
 */
class NodeInfoOverlay(inspector: FloatingInspector) :
    BaseOverlay<OverlayNodeInfoBinding>(inspector) {

    private val uncheckedOptions = mutableSetOf<AppletOption>()

    private val uiObjectFactory = UiObjectAppletFactory(FlowFactory.ID_UI_OBJECT_APPLET_FACTORY)

    private val pkgFactory = PackageAppletFactory(FlowFactory.ID_PKG_APPLET_FACTORY)

    private val options = mutableListOf<AppletOption>()

    private var checkedOptions = emptyList<AppletOption>()

    private val realSize by lazy {
        val size = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealSize(size)
        size
    }

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.width = WindowManager.LayoutParams.MATCH_PARENT
        base.height = WindowManager.LayoutParams.MATCH_PARENT
    }

    private val adapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(options, ItemNodeInfoBinding::class.java, {
            binding.root.setOnClickListener {
                val option = options[adapterPosition]
                if (uncheckedOptions.contains(option)) {
                    uncheckedOptions.remove(option)
                } else {
                    uncheckedOptions.add(option)
                }
                adapter.notifyItemChanged(adapterPosition, true)
            }
        }) { b, _, p ->
            b.tvAttrName.text = p.title
            b.tvAttrValue.text = p.description
            b.checkbox.isChecked = !uncheckedOptions.contains(p)
        }
    }

    private fun collectProperties() {
        val node = vm.emphaticNode.require().source
        vm.currentComp.value?.let {
            options.add(pkgFactory.pkgCollection.withValue(Collections.singletonList(it.pkgName)))
            options.add(pkgFactory.activityCollection.withValue(Collections.singletonList(it.actName)))
        }
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

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val startIndex = options.size
        options.add(uiObjectFactory.left.withValue(Distance.exactPxInScreen(bounds.left)))
        options.add(uiObjectFactory.right.withValue(Distance.exactPxInScreen(realSize.x - bounds.right)))
        options.add(uiObjectFactory.top.withValue(Distance.exactPxInScreen(bounds.top)))
        options.add(uiObjectFactory.bottom.withValue(Distance.exactPxInScreen(realSize.y - bounds.bottom)))
        options.add(uiObjectFactory.width.withValue(Distance.exactPxInScreen(bounds.width())))
        options.add(uiObjectFactory.height.withValue(Distance.exactPxInScreen(bounds.height())))
        val pxDistanceDescriber = { distance: Distance ->
            R.string.format_px.format(distance.rangeStart.toInt())
        }
        for (i in startIndex..options.lastIndex) {
            options[i].withDescriber(pxDistanceDescriber)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOverlayInflated() {
        super.onOverlayInflated()
        uiObjectFactory.brandAll()
        pkgFactory.brandAll()
        binding.btnCancel.setOnClickListener {
            vm.showNodeInfo.value = false
        }
        binding.btnComplete.setOnClickListener {
            context.routeTo(Router.HOST_ACCEPT_NODE_INFO_FROM_INSPECTOR)
            checkedOptions = options - uncheckedOptions
            vm.isCollapsed.value = true
            vm.showNodeInfo.value = false
        }
        binding.container.background = context.createMaterialShapeDrawable()
        inspector.observe(vm.showNodeInfo) {
            if (!it) {
                animateHide()
                options.clear()
                uncheckedOptions.clear()
            } else {
                animateShow()
                collectProperties()
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