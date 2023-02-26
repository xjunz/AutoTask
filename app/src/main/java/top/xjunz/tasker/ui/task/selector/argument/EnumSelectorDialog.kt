/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.os.Bundle
import android.view.View
import androidx.annotation.ArrayRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogEnumSelectorBinding
import top.xjunz.tasker.databinding.ItemEnumSelectorBinding
import top.xjunz.tasker.ktx.array
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.observeNostalgic
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/10/27
 */
class EnumSelectorDialog : BaseDialogFragment<DialogEnumSelectorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        var isSingleSelection = false

        var spanCount = 3

        var title: CharSequence? = null

        lateinit var doOnCompletion: (Collection<Int>) -> Unit

        lateinit var enum: Array<out CharSequence>

        val selectedIndexes = mutableSetOf<Int>()

        val changedIndex = MutableLiveData<Int>()

        fun toggleSelection(index: Int) {
            if (selectedIndexes.contains(index)) {
                selectedIndexes.remove(index)
            } else {
                if (isSingleSelection) {
                    selectedIndexes.clear()
                }
                selectedIndexes.add(index)
            }
            changedIndex.value = index
        }

        fun complete() {
            doOnCompletion(selectedIndexes.sorted())
        }
    }

    private val viewModel by viewModels<InnerViewModel>()

    private val adapter: Adapter<*> by lazy {
        inlineAdapter(viewModel.enum, ItemEnumSelectorBinding::class.java, {
            itemView.setNoDoubleClickListener {
                viewModel.toggleSelection(adapterPosition)
            }
        }) { b, p, i ->
            b.tvTitle.text = i
            b.root.isSelected = viewModel.selectedIndexes.contains(p)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (binding.rvEnum.layoutManager as GridLayoutManager).spanCount = viewModel.spanCount
        binding.rvEnum.adapter = adapter
        binding.tvTitle.text = viewModel.title
        binding.btnComplete.setNoDoubleClickListener {
            if (viewModel.selectedIndexes.isEmpty()) {
                toastAndShake(R.string.error_no_selection)
            } else {
                viewModel.complete()
                dismiss()
            }
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        observeNostalgic(viewModel.changedIndex) { prev, cur ->
            if (prev != null && prev != cur) {
                adapter.notifyItemChanged(prev)
            }
            adapter.notifyItemChanged(cur)
        }
    }

    fun setInitialSelections(selections: Collection<Int>?): EnumSelectorDialog = doWhenCreated {
        if (selections != null) viewModel.selectedIndexes.addAll(selections)
    }

    fun setSpanCount(span: Int) = doWhenCreated {
        viewModel.spanCount = span
    }

    fun init(
        title: CharSequence?, @ArrayRes res: Int, onCompletion: (Collection<Int>) -> Unit
    ): EnumSelectorDialog = doWhenCreated {
        viewModel.title = title
        viewModel.enum = res.array
        viewModel.doOnCompletion = onCompletion
    }

    fun init(
        title: CharSequence?,
        array: Array<out CharSequence>,
        onCompletion: (Collection<Int>) -> Unit
    ): EnumSelectorDialog = doWhenCreated {
        viewModel.title = title
        viewModel.enum = array
        viewModel.doOnCompletion = onCompletion
    }

    fun setSingleSelectionMode() = doWhenCreated {
        viewModel.isSingleSelection = true
    }
}