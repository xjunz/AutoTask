/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionSet
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R.style.TextAppearance_Material3_LabelLarge
import com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
import com.google.android.material.transition.platform.MaterialFadeThrough
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogAppletSelectorBinding
import top.xjunz.tasker.databinding.ItemAppletFactoryBinding
import top.xjunz.tasker.databinding.ItemAppletOptionBinding
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ContainerFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.isPremium
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.common.PreferenceHelpDialog
import top.xjunz.tasker.ui.main.ColorScheme
import top.xjunz.tasker.ui.main.EventCenter.doOnEventReceived
import top.xjunz.tasker.ui.main.EventCenter.doOnEventRoutedWithValue
import top.xjunz.tasker.ui.purchase.PurchaseDialog.Companion.showPurchaseDialog
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.ui.task.showcase.TaskCreatorDialog
import top.xjunz.tasker.upForGrabs
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/09/26
 */
class AppletSelectorDialog : BaseDialogFragment<DialogAppletSelectorBinding>() {

    override val isFullScreen: Boolean = true

    private val viewModel by viewModels<AppletSelectorViewModel>()

    private val rvBottom: RecyclerView get() = binding.shoppingCart.rvBottom

    private val leftAdapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(
            viewModel.registryOptions, ItemAppletFactoryBinding::class.java, {
                itemView.setNoDoubleClickListener {
                    viewModel.selectFlowRegistry(adapterPosition)
                }
            }) { binding, index, data ->
            binding.tvLabel.text = data.rawTitle
            binding.tvLabel.isSelected = viewModel.selectedFlowRegistry eq index
        }
    }

    private val optionClickListener by lazy {
        AppletOptionClickHandler(childFragmentManager)
    }

    private val shopCartIntegration by lazy {
        ShoppingCartIntegration(binding.shoppingCart, viewModel, binding.rvRight)
    }

    private val rightAdapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(viewModel.options, ItemAppletOptionBinding::class.java, {
            itemView.setNoDoubleClickListener {
                val position = adapterPosition
                val option = viewModel.options[position]
                if (!isPremium && option.isPremiumOnly) {
                    showPurchaseDialog(R.string.tip_premium_only_applet)
                } else if (option.isValid) {
                    viewModel.requestAppendOption.value = option to position
                }
            }
            binding.ibInvert.setNoDoubleClickListener {
                viewModel.options[adapterPosition].toggleInversion()
                rightAdapter.notifyItemChanged(adapterPosition, true)
            }
        }) { binding, position, option ->
            var title = option.loadUnspannedTitle(null)
            val m = option.titleModifier
            if (m != null) title = title?.plus(
                " ($m)".foreColored(ColorScheme.textColorDisabled).relativeSize(.9F)
            )
            binding.tvLabel.text = title
            binding.ibInvert.isVisible = option.isInvertible
            if (!option.isValid) {
                binding.tvLabel.setTextAppearance(TextAppearance_Material3_TitleMedium)
                binding.tvLabel.setTextColor(ColorScheme.colorPrimary)
            } else {
                binding.tvLabel.setTextAppearance(TextAppearance_Material3_LabelLarge)
                binding.tvLabel.setTextColor(ColorScheme.colorOnSurface)
            }
            binding.tvBadge.isVisible = false
            binding.tvBadge.text = null
            if (option.isShizukuOnly) {
                binding.tvBadge.isVisible = true
                binding.tvBadge.text = R.string.shizuku.text
            }
            if (option.minApiLevel != -1 && Build.VERSION.SDK_INT < option.minApiLevel) {
                binding.tvBadge.isVisible = true
                if (binding.tvBadge.text.isNullOrEmpty()) {
                    binding.tvBadge.text = R.string.format_api_level.format(option.minApiLevel)
                } else {
                    binding.tvBadge.text += " | " + R.string.format_api_level.format(option.minApiLevel)
                }
            }
            if (option.isPremiumOnly && !upForGrabs) {
                binding.tvBadge.isVisible = true
                if (binding.tvBadge.text.isNullOrEmpty()) {
                    binding.tvBadge.text = R.string.premium.str
                } else {
                    binding.tvBadge.text += " | " + R.string.premium.str
                }
            }
            if (viewModel.animateItems) {
                val offset = 30L
                val anim = AnimationUtils.loadAnimation(context, R.anim.mtrl_item_ease_enter)
                anim.startOffset = (offset + position) * position
                binding.root.startAnimation(anim)
                if (position == 0) viewModel.viewModelScope.launch {
                    delay(offset)
                    viewModel.animateItems = false
                }
            }
        }
    }

    private val bottomAdapter by lazy {
        AppletCandidatesAdapter(viewModel, optionClickListener)
    }

    fun init(
        origin: Flow,
        substitutionMode: Boolean = false,
        doOnCompletion: (List<Applet>) -> Unit
    ) = doWhenCreated {
        viewModel.isSubstitutionMode = substitutionMode
        viewModel.onCompletion = doOnCompletion
        viewModel.setScope(origin)
        viewModel.flow = ContainerFlow().also {
            it.maxSize = origin.maxSize - origin.size
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppletOptionFactory.resetAll()
        if (viewModel.selectedFlowRegistry.isNull()) {
            viewModel.selectFlowRegistry(0)
        }
        binding.tvTitle.text = viewModel.title
        shopCartIntegration.init(this)
        binding.topBar.oneShotApplySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.ibDismiss.setOnClickListener {
            dismiss()
        }
        binding.shoppingCart.circularRevealContainer.doOnPreDraw {
            binding.rvRight.updatePadding()
        }
        binding.shoppingCart.btnCount.setNoDoubleClickListener {
            if (viewModel.flow.isNotEmpty())
                viewModel.showClearConfirmation.value = true
        }
        binding.shoppingCart.btnComplete.setNoDoubleClickListener {
            viewModel.complete()
            dismiss()
        }
        binding.cvHeader.setNoDoubleClickListener { v ->
            FloatingInspectorDialog().setMode(v.tag.casted()).show(parentFragmentManager)
        }
        binding.shoppingCart.rvBottom.adapter = bottomAdapter
        observeLiveData()

        when (TaskCreatorDialog.REQUESTED_QUICK_TASK_CREATOR) {
            TaskCreatorDialog.QUICK_TASK_CREATOR_GESTURE_RECORDER -> {
                // Navigate to gesture actions
                viewModel.selectFlowRegistry(
                    viewModel.registryOptions.indexOf(AppletOptionFactory.flowRegistry.gestureActions)
                )
                // Request append custom gesture option
                val gestureOption = AppletOptionFactory.gestureActionRegistry.performCustomGestures
                viewModel.requestAppendOption.value =
                    gestureOption to AppletOptionFactory.gestureActionRegistry
                        .categorizedOptions.indexOf(gestureOption)
            }

            TaskCreatorDialog.QUICK_TASK_CREATOR_CLICK_AUTOMATION -> {
                viewModel.selectFlowRegistry(
                    viewModel.registryOptions.indexOf(AppletOptionFactory.flowRegistry.gestureActions)
                )
                val gestureOption = AppletOptionFactory.gestureActionRegistry.click
                viewModel.requestAppendOption.value =
                    gestureOption to AppletOptionFactory.gestureActionRegistry
                        .categorizedOptions.indexOf(gestureOption)
            }

            TaskCreatorDialog.QUICK_TASK_CREATOR_AUTO_CLICK -> {
                FloatingInspectorDialog().setMode(InspectorMode.UI_OBJECT)
                    .show(childFragmentManager)
                toast(R.string.tip_select_ui_object_from_screen)
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
            val flowRegistry = AppletOptionFactory.flowRegistry
            if (viewModel.registryOptions[it] == flowRegistry.uiObjectCriteria) {
                binding.cvHeader.tag = InspectorMode.UI_OBJECT
                binding.cvHeader.isVisible = true
                binding.tvHeader.text =
                    R.string.format_enable_floating_inspector.formatAsHtml(InspectorMode.UI_OBJECT.label)
            } else binding.cvHeader.isVisible = false
        }
        observe(viewModel.applets) {
            binding.shoppingCart.btnCount.text = R.string.format_clear_applets.format(it.size)
            if (it.isEmpty()) shopCartIntegration.collapse()
            bottomAdapter.submitList(it)
        }
        observeTransient(viewModel.onAppletChanged) {
            bottomAdapter.notifyItemChanged(bottomAdapter.currentList.indexOf(it), true)
        }
        observeTransient(viewModel.onAppletAdded) {
            val vh = binding.rvRight.findViewHolderForAdapterPosition(it)
            if (vh != null) shopCartIntegration.animateIntoShopCart(vh.itemView)
        }
        observeTransient(viewModel.requestAppendOption) {
            val applet = it.first.yield()
            optionClickListener.onClick(applet, it.first) {
                viewModel.notifyOptionClicked(applet, it.second)
            }
        }
        observeDangerousConfirmation(
            viewModel.showClearConfirmation, R.string.prompt_clear_all_options, R.string.clear_all
        ) {
            viewModel.clearAllCandidates()
        }
        doOnEventReceived<Applet>(AppletOption.EVENT_TOGGLE_RELATION) {
            it.toggleRelation()
            if (it is Action) {
                PreferenceHelpDialog().init(
                    R.string.tip, R.string.tip_applet_relation, Preferences.showToggleRelationTip
                ) { noMore ->
                    Preferences.showToggleRelationTip = !noMore
                }.show(childFragmentManager)
            }
            bottomAdapter.notifyItemChanged(viewModel.applets.require().indexOf(it), true)
        }
        doOnEventRoutedWithValue<List<Applet>>(FloatingInspector.EVENT_NODE_INFO_SELECTED) {
            if (TaskCreatorDialog.REQUESTED_QUICK_TASK_CREATOR == TaskCreatorDialog.QUICK_TASK_CREATOR_AUTO_CLICK) {
                viewModel.acceptAppletsFromAutoClick(it)
            } else {
                viewModel.acceptApplets(it)
            }
            shopCartIntegration.expand()
            rvBottom.scrollToPosition(bottomAdapter.itemCount)
        }
    }
}