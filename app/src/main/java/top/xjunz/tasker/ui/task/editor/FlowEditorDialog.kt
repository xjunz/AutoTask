package top.xjunz.tasker.ui.task.editor

import android.annotation.SuppressLint
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionSet
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.MaterialFadeThrough
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogAppletShoppingCartBinding
import top.xjunz.tasker.databinding.ItemAppletFactoryBinding
import top.xjunz.tasker.databinding.ItemAppletOptionBinding
import top.xjunz.tasker.engine.flow.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.factory.AppletOption
import top.xjunz.tasker.task.factory.AppletRegistry
import top.xjunz.tasker.task.factory.AppletRegistry.Companion.appletId
import top.xjunz.tasker.task.factory.FlowFactory
import top.xjunz.tasker.task.inspector.InspectorController
import top.xjunz.tasker.ui.ColorSchemes
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog

/**
 * @author xjunz 2022/09/26
 */
class FlowEditorDialog : BaseDialogFragment<DialogAppletShoppingCartBinding>() {

    override val isFullScreen: Boolean = true

    private class InnerViewModel : ViewModel() {

        var shouldAnimateItem = true

        val appletRegistry = AppletRegistry()

        var previousSelectedFactory = -1

        val selectedFactory = MutableLiveData<Int>()

        val options = mutableListOf<AppletOption>()

        private val _candidates = mutableListOf<Flow>()

        val candidates = MutableLiveData<List<Flow>>(_candidates)

        lateinit var title: CharSequence

        fun singleSelectFactory(index: Int) {
            if (selectedFactory.value == index) return
            options.clear()
            options.addAll(
                appletRegistry.findFactoryById(
                    appletRegistry.flowFactory.options[index].appletId
                ).categorizedOptions
            )
            previousSelectedFactory = selectedFactory.value ?: 0
            selectedFactory.value = index
        }

        fun appendApplet(option: AppletOption): Boolean {
            if (!option.isValid) return false
            val flowOption = appletRegistry.findFlowOption(option.factoryId)
            if (_candidates.isEmpty() || _candidates.last().appletId != flowOption.appletId) {
                // append a flow if needed
                _candidates.add(flowOption.createApplet() as Flow)
            }
            _candidates.last().applets.add(option.createApplet())
            candidates.value = _candidates
            return true
        }
    }

    private val viewModel by viewModels<InnerViewModel>()

    private val leftAdapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(
            viewModel.appletRegistry.flowFactory.options,
            ItemAppletFactoryBinding::class.java, {
                itemView.setOnClickListener {
                    viewModel.singleSelectFactory(adapterPosition)
                }
            }) { binding, index, data ->
            binding.tvLabel.text = data.title
            binding.tvLabel.isSelected = index == viewModel.selectedFactory.require()
        }
    }

    private val shopCartIntegration by lazy {
        ShoppingCartIntegration(binding.shoppingCart, binding.rvRight)
    }

    private val rightAdapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(viewModel.options, ItemAppletOptionBinding::class.java, {
            itemView.setOnClickListener {
                if (shopCartIntegration.isAnimatorRunning) return@setOnClickListener
                val option = viewModel.options[adapterPosition]
                AppletOptionOnClickListener.onClick(option, this@FlowEditorDialog)
            }
            binding.ibInvert.setOnClickListener {
                viewModel.options[adapterPosition].toggleInverted()
                rightAdapter.notifyItemChanged(adapterPosition, true)
            }
        }) { binding, position, option ->
            binding.tvLabel.text = option.currentTitle
            binding.ibInvert.isInvisible = !option.isInvertible
            if (!option.isValid) {
                binding.tvLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                binding.tvLabel.setTextColor(ColorSchemes.colorPrimary)
            } else {
                binding.tvLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                binding.tvLabel.setTextColor(ColorSchemes.colorOnSurface)
            }
            if (viewModel.shouldAnimateItem) {
                val staggerAnimOffsetMills = 30L
                val easeIn = AnimationUtils.loadAnimation(context, R.anim.mtrl_item_ease_enter)
                easeIn.startOffset = (staggerAnimOffsetMills + position) * position
                binding.root.startAnimation(easeIn)
                if (position == 0) {
                    viewModel.viewModelScope.launch {
                        delay(staggerAnimOffsetMills)
                        viewModel.shouldAnimateItem = false
                    }
                }
            }
        }
    }

    private val bottomAdapter by lazy { AppletCandidatesAdapter(viewModel.appletRegistry) }

    fun setTitle(title: CharSequence): FlowEditorDialog {
        doOnCreated {
            viewModel.title = title
        }
        return this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.selectedFactory.value == null) {
            viewModel.singleSelectFactory(0)
        }
        binding.tvTitle.text = viewModel.title
        shopCartIntegration.init(this)
        binding.shoppingCart.rvBottom.adapter = bottomAdapter
        binding.tvTitle.oneShotApplySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        observeLiveData()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeLiveData() {
        val transition = TransitionSet()
        transition.addTransition(ChangeBounds().addTarget(binding.rvRight))
        transition.addTransition(MaterialFadeThrough().addTarget(binding.cvHeader))
        observe(viewModel.selectedFactory) {
            if (binding.rvLeft.adapter == null) {
                binding.rvLeft.adapter = leftAdapter
            } else {
                leftAdapter.notifyItemChanged(viewModel.previousSelectedFactory, true)
                leftAdapter.notifyItemChanged(it, true)
            }
            if (binding.rvRight.adapter == null) {
                binding.rvRight.adapter = rightAdapter
            } else {
                viewModel.shouldAnimateItem = true
                rightAdapter.notifyDataSetChanged()
            }
            binding.rootView.beginAutoTransition(transition)
            when (viewModel.appletRegistry.flowFactory.options[it].appletId) {
                FlowFactory.ID_PKG_APPLET_FACTORY -> {
                    binding.cvHeader.isVisible = true
                    binding.tvHeader.text = R.string.tip_enable_floating_comp_inspector.text
                }
                FlowFactory.ID_UI_OBJECT_APPLET_FACTORY -> {
                    binding.cvHeader.isVisible = true
                    binding.tvHeader.text = R.string.tip_enable_floating_ui_inspector.text
                }
                else -> {
                    binding.cvHeader.isVisible = false
                }
            }
            binding.cvHeader.setOnClickListener {
                if (InspectorController.isReady()) {
                    InspectorController.showInspector()
                } else {
                    FloatingInspectorDialog().show(parentFragmentManager)
                }
            }
        }
        observe(viewModel.candidates) { list ->
            bottomAdapter.updateApplets(list)
            binding.shoppingCart.btnCount.text =
                R.string.format_applet_count.format(list.sumOf { it.applets.size })
        }
    }
}