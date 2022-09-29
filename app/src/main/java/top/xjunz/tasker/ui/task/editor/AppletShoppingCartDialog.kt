package top.xjunz.tasker.ui.task.editor

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import top.xjunz.tasker.colorSchemes
import top.xjunz.tasker.databinding.DialogAppletShoppingCartBinding
import top.xjunz.tasker.databinding.ItemAppletFactoryBinding
import top.xjunz.tasker.databinding.ItemAppletOptionBinding
import top.xjunz.tasker.ktx.doOnCreated
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.task.factory.AppletOption
import top.xjunz.tasker.task.factory.AppletRegistry
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.ui.base.inlineAdapter

/**
 * @author xjunz 2022/09/26
 */
class AppletShoppingCartDialog : BaseBottomSheetDialog<DialogAppletShoppingCartBinding>() {

    private class InnerViewModel : ViewModel() {

        val appletRegistry = AppletRegistry()

        var previousSelectedFactory = -1

        var selectedFactory = MutableLiveData<Int>()

        val options = mutableListOf<AppletOption>()

        lateinit var title: CharSequence

        fun singleSelectFactory(index: Int) {
            if (selectedFactory.value == index) return
            previousSelectedFactory = selectedFactory.value ?: 0
            selectedFactory.value = index
            options.clear()
            options.addAll(appletRegistry.allFactories[selectedFactory.require()].categorizedAppletOptions)
        }
    }

    private val viewModel by viewModels<InnerViewModel>()

    private val leftAdapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(viewModel.appletRegistry.allFactories, ItemAppletFactoryBinding::class.java, {
            itemView.setOnClickListener {
                viewModel.singleSelectFactory(adapterPosition)
            }
        }) { binding, index, data ->
            binding.tvLabel.setText(data.label)
            binding.tvLabel.isSelected = index == viewModel.selectedFactory.require()
        }
    }

    private val rightAdapter: RecyclerView.Adapter<*> by lazy {
        inlineAdapter(viewModel.options, ItemAppletOptionBinding::class.java, {
            itemView.setOnClickListener {
                // viewModel.singleSelectFactory(adapterPosition)
            }
            binding.ibInvert.setOnClickListener {
                viewModel.options[adapterPosition].toggleInverted()
                rightAdapter.notifyItemChanged(adapterPosition, true)
            }
        }) { binding, _, data ->
            binding.tvLabel.text = if (data.isInverted) data.invertedLabel else data.label
            //binding.ibAdd.isVisible = data.isValid
            binding.ibInvert.isVisible = data.isInvertible
            if (!data.isValid) {
                binding.tvLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                binding.tvLabel.setTextColor(colorSchemes.colorPrimary)
            } else {
                binding.tvLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                binding.tvLabel.setTextColor(colorSchemes.colorOnSurface)
            }
        }
    }

    fun setTitle(title: CharSequence): AppletShoppingCartDialog {
        doOnCreated {
            viewModel.title = title
        }
        return this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = viewModel.title
        viewModel.singleSelectFactory(0)
        observeLiveData()
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeLiveData() {
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
                rightAdapter.notifyDataSetChanged()
            }
        }
    }
}