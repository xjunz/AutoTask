package top.xjunz.tasker.ui.task.selector

import android.annotation.SuppressLint
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionSet
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.doOnPreDraw
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R.style.TextAppearance_Material3_LabelLarge
import com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
import com.google.android.material.transition.platform.MaterialFadeThrough
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogAppletSelectorBinding
import top.xjunz.tasker.databinding.ItemAppletFactoryBinding
import top.xjunz.tasker.databinding.ItemAppletOptionBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.service.isFloatingInspectorShown
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.ui.MainViewModel
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener
import top.xjunz.tasker.util.Router

/**
 * @author xjunz 2022/09/26
 */
class AppletSelectorDialog : BaseDialogFragment<DialogAppletSelectorBinding>() {

    override val isFullScreen: Boolean = true

    private val viewModel by viewModels<AppletSelectorViewModel>()

    private val rvBottom: RecyclerView get() = binding.shoppingCart.rvBottom

    private val mainViewModel: MainViewModel by activityViewModels()

    private val leftAdapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(
            viewModel.registryOptions, ItemAppletFactoryBinding::class.java, {
                itemView.setOnClickListener {
                    viewModel.selectFlowRegistry(adapterPosition)
                }
            }) { binding, index, data ->
            binding.tvLabel.text = data.rawTitle
            binding.tvLabel.isSelected = viewModel.selectedFlowRegistry eq index
        }
    }

    private val onOptionClickListener by lazy {
        AppletOptionOnClickListener(childFragmentManager, viewModel.appletOptionFactory)
    }

    private val shopCartIntegration by lazy {
        ShoppingCartIntegration(binding.shoppingCart, viewModel, binding.rvRight)
    }

    private val rightAdapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(viewModel.options, ItemAppletOptionBinding::class.java, {
            itemView.setOnClickListener {
                if (shopCartIntegration.isAnimatorRunning) return@setOnClickListener
                val position = adapterPosition
                val option = viewModel.options[position]
                if (option.isValid) {
                    val applet = option.yield()
                    onOptionClickListener.onClick(applet, option) {
                        viewModel.appendApplet(applet)
                        viewModel.onAppletAdded.value = position
                    }
                }
            }
            binding.ibInvert.setOnClickListener {
                viewModel.options[adapterPosition].toggleInversion()
                rightAdapter.notifyItemChanged(adapterPosition, true)
            }
        }) { binding, position, option ->
            var title = option.currentTitle
            val m = option.titleModifier
            if (m != null) {
                title = title?.plus(
                    " ($m)".foreColored(ColorScheme.textColorDisabled).relativeSize(.9F)
                )
            }
            binding.tvLabel.text = title
            binding.ibInvert.isInvisible = !option.isInvertible
            if (!option.isValid) {
                binding.tvLabel.setTextAppearance(TextAppearance_Material3_TitleMedium)
                binding.tvLabel.setTextColor(ColorScheme.colorPrimary)
            } else {
                binding.tvLabel.setTextAppearance(TextAppearance_Material3_LabelLarge)
                binding.tvLabel.setTextColor(ColorScheme.colorOnSurface)
            }
            if (viewModel.animateItems) {
                val staggerAnimOffsetMills = 30L
                val easeIn = AnimationUtils.loadAnimation(context, R.anim.mtrl_item_ease_enter)
                easeIn.startOffset = (staggerAnimOffsetMills + position) * position
                binding.root.startAnimation(easeIn)
                if (position == 0)
                    viewModel.viewModelScope.launch {
                        delay(staggerAnimOffsetMills)
                        viewModel.animateItems = false
                    }
            }
        }
    }

    private val bottomAdapter by lazy {
        AppletCandidatesAdapter(viewModel, onOptionClickListener)
    }

    fun doOnCompletion(block: (List<Applet>) -> Unit) = doWhenCreated {
        viewModel.onCompletion = block
    }

    fun scopedBy(flow: Flow): AppletSelectorDialog = doWhenCreated {
        viewModel.setScope(flow)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.selectedFlowRegistry.isNull()) {
            viewModel.selectFlowRegistry(0)
        }
        if (savedInstanceState == null)
            viewModel.flow = Flow()

        binding.tvTitle.text = viewModel.title
        shopCartIntegration.init(this)
        binding.tvTitle.oneShotApplySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.shoppingCart.circularRevealContainer.doOnPreDraw {
            binding.rvRight.updatePadding()
        }
        binding.shoppingCart.btnCount.setOnClickListener {
            if (viewModel.flow.isNotEmpty())
                viewModel.showClearDialog.value = true
        }
        binding.shoppingCart.btnComplete.setOnClickListener {
            viewModel.complete()
            dismiss()
        }
        binding.cvHeader.setAntiMoneyClickListener { v ->
            val mode = v.tag.casted<InspectorMode>()
            if (isFloatingInspectorShown) {
                if (floatingInspector.mode != mode) {
                    floatingInspector.mode = mode
                    toast(R.string.format_switch_mode.format(mode.label))
                    requireActivity().pressHome()
                }
            } else {
                FloatingInspectorDialog().setMode(mode).doOnSucceeded {
                    floatingInspector.viewModel.showExtraOptions = !viewModel.isScoped
                }.show(parentFragmentManager)
            }
        }
        binding.shoppingCart.rvBottom.adapter = bottomAdapter
        observeLiveData()
        if (viewModel.isScoped) binding.cvHeader.doOnPreDraw {
            if (isFloatingInspectorShown) {
                floatingInspector.viewModel.showExtraOptions = false
                val mode = it.tag?.casted<InspectorMode>() ?: return@doOnPreDraw
                if (floatingInspector.mode != mode) {
                    floatingInspector.mode = mode
                    toast(R.string.format_switch_mode.format(mode.label))
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeLiveData() {
        observeNostalgic(viewModel.selectedFlowRegistry) { prev, it ->
            if (binding.rvLeft.adapter == null) {
                binding.rvLeft.adapter = leftAdapter
            } else {
                leftAdapter.notifyItemChanged(prev!!, true)
                leftAdapter.notifyItemChanged(it, true)
            }
            if (binding.rvRight.adapter == null) {
                binding.rvRight.adapter = rightAdapter
            } else {
                viewModel.animateItems = true
                rightAdapter.notifyDataSetChanged()
                binding.rvRight.scrollToPosition(0)
            }
            val transition = TransitionSet()
            transition.addTransition(ChangeBounds().addTarget(binding.rvRight))
            transition.addTransition(MaterialFadeThrough().addTarget(binding.cvHeader))
            binding.rootView.beginAutoTransition(transition)
            val flowRegistry = viewModel.appletOptionFactory.flowRegistry
            when (viewModel.registryOptions[it]) {
                flowRegistry.componentFlow -> {
                    binding.cvHeader.tag = InspectorMode.COMPONENT
                    binding.cvHeader.isVisible = true
                    binding.tvHeader.text =
                        R.string.format_enable_floating_inspector.formatAsHtml(InspectorMode.COMPONENT.label)
                }
                flowRegistry.uiObjectFlow -> {
                    binding.cvHeader.tag = InspectorMode.UI_OBJECT
                    binding.cvHeader.isVisible = true
                    binding.tvHeader.text =
                        R.string.format_enable_floating_inspector.formatAsHtml(InspectorMode.UI_OBJECT.label)
                }
                else -> binding.cvHeader.isVisible = false
            }
        }
        observe(viewModel.applets) {
            binding.shoppingCart.btnCount.text =
                R.string.format_clear_applets.format(it.size)
            if (it.isEmpty())
                shopCartIntegration.collapse()
            bottomAdapter.submitList(it)
        }
        observeTransient(viewModel.onAppletChanged) {
            bottomAdapter.notifyItemChanged(bottomAdapter.currentList.indexOf(it))
        }
        observeTransient(viewModel.onAppletAdded) {
            val vh = binding.rvRight.findViewHolderForAdapterPosition(it)
            if (vh != null) shopCartIntegration.animateIntoShopCart(vh.itemView)
        }
        observeConfirmation(viewModel.showClearDialog, R.string.prompt_clear_all_options) {
            viewModel.clearAllCandidates()
        }
        mainViewModel.doOnAction(this, AppletOption.ACTION_TOGGLE_RELATION) {
            val hashcode = it.toInt()
            val index = viewModel.applets.require().indexOfFirst { applet ->
                applet.hashCode() == hashcode
            }
            viewModel.applets.require()[index].toggleRelation()
            bottomAdapter.notifyItemChanged(index)
        }
        mainViewModel.doOnHostRouted(this, Router.HOST_ACCEPT_OPTIONS_FROM_INSPECTOR) {
            val prevSize = bottomAdapter.itemCount
            viewModel.acceptAppletsFromInspector()
            shopCartIntegration.expand()
            rvBottom.scrollToPosition(prevSize)
        }
    }
}