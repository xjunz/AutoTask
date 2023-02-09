/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView.Adapter
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogVarargTextEditorBinding
import top.xjunz.tasker.databinding.ItemVarargTextBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.descriptor.ArgumentDescriptor
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2023/02/05
 */
class VarargTextEditorDialog : BaseDialogFragment<DialogVarargTextEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        var title: CharSequence? = null

        var args: MutableList<Arg> = mutableListOf()

        val onArgChanged = MutableLiveData<Int>()

        lateinit var argDescriptor: ArgumentDescriptor

        lateinit var applet: Applet

        lateinit var onCompletion: (value: String, referents: List<String>) -> Unit

        fun setInitialApplet(initial: Applet) {
            applet = initial
            val refNames = initial.references.values.toList()
            val texts = initial.value?.casted<String>()?.split("%s")
            if (texts != null) {
                var index = 0
                var exhuasted = false
                while (!exhuasted) {
                    exhuasted = true
                    if (index % 2 == 0) {
                        if (texts.lastIndex >= index / 2) {
                            exhuasted = false
                            if (texts[index / 2].isNotEmpty()) {
                                args.add(Arg(null, texts[index / 2]))
                            }
                        }
                    } else {
                        if (refNames.lastIndex >= (index - 1) / 2) {
                            exhuasted = false
                            args.add(Arg(refNames[(index - 1) / 2], null))
                        }
                    }
                    index++
                }
            }
            if (args.isEmpty()) {
                // Add an empty arg
                args.add(Arg())
            }
        }
    }

    private data class Arg(
        var referentName: String? = null,
        var text: String? = null
    ) {
        val isReference: Boolean get() = referentName != null
        val isSpecified: Boolean get() = referentName != null || text != null
    }

    private val viewModel by viewModels<InnerViewModel>()

    private val adapter: Adapter<*> by lazy {
        inlineAdapter(viewModel.args, ItemVarargTextBinding::class.java, {
            binding.root.setAntiMoneyClickListener {
                val position = adapterPosition
                val arg = viewModel.args[position]
                if (arg.isSpecified) {
                    if (arg.isReference) {
                        showReferenceSelectorDialog(position)
                    } else {
                        showTextInputDialog(position)
                    }
                } else {
                    val popup = PopupMenu(requireContext(), binding.root, Gravity.END)
                    popup.menu.add(R.string.edit)
                    popup.menu.add(R.string.refer_to)
                    popup.setOnMenuItemClickListener { item ->
                        when (item.title?.toString()) {
                            R.string.edit.str -> showTextInputDialog(adapterPosition)
                            R.string.refer_to.str -> showReferenceSelectorDialog(adapterPosition)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    popup.show()
                }
            }
            binding.ibAddAfter.setAntiMoneyClickListener {
                val emptyArg = Arg()
                if (adapterPosition == viewModel.args.lastIndex) {
                    viewModel.args.add(emptyArg)
                } else {
                    viewModel.args.add(adapterPosition + 1, emptyArg)
                }
                adapter.notifyItemInserted(adapterPosition + 1)
            }
            binding.ibRemove.setAntiMoneyClickListener {
                if (viewModel.args.size == 1) {
                    if (viewModel.args[0].isSpecified) {
                        viewModel.args[0] = Arg()
                        adapter.notifyItemChanged(0)
                    }
                } else {
                    viewModel.args.removeAt(adapterPosition)
                    adapter.notifyItemRemoved(adapterPosition)
                }
            }
        }) { binding, _, arg ->
            if (arg.isSpecified) {
                if (arg.isReference) {
                    binding.tvArg.text = arg.referentName?.foreColored()
                    binding.tvArg.setDrawableStart(R.drawable.ic_baseline_link_24)
                } else {
                    binding.tvArg.setTextColor(ColorScheme.textColorPrimary)
                    binding.tvArg.setDrawableStart(R.drawable.ic_text_fields_24px)
                    binding.tvArg.text = arg.text
                }
            } else {
                binding.tvArg.text = R.string.unspecified.text.italic()
                binding.tvArg.setDrawableStart(R.drawable.ic_edit_24dp)
            }
        }
    }

    private lateinit var pvm: FlowEditorViewModel

    private val gvm get() = pvm.global

    private fun showReferenceSelectorDialog(whichArg: Int) {
        val applet = viewModel.applet
        FlowEditorDialog().init(pvm.task, gvm.root, true, gvm)
            .setArgumentToSelect(
                applet,
                viewModel.argDescriptor,
                viewModel.args[whichArg].referentName
            )
            .doOnArgumentSelected { referent ->
                viewModel.args[whichArg].referentName = referent
                viewModel.onArgChanged.value = whichArg
            }.show(childFragmentManager)
    }

    private fun showTextInputDialog(position: Int) {
        val arg = viewModel.args[position]
        TextEditorDialog().init(viewModel.argDescriptor.name, arg.text) {
            arg.text = it
            viewModel.onArgChanged.value = position
            return@init null
        }.show(childFragmentManager)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pvm = peekParentViewModel<FlowEditorDialog, FlowEditorViewModel>()
        binding.rvArguments.adapter = adapter
        binding.btnNegative.setAntiMoneyClickListener {
            dismiss()
        }
        binding.tvTitle.text = viewModel.title
        binding.btnPositive.setAntiMoneyClickListener {
            val unspecifiedIndex = viewModel.args.indexOfFirst { !it.isSpecified }
            if (unspecifiedIndex >= 0) {
                binding.rvArguments.scrollPositionToCenterVertically(unspecifiedIndex, true) {
                    it.shake()
                    toast(R.string.error_unspecified)
                }
            } else {
                val args = mutableListOf<String>()
                val value = viewModel.args.joinToString(separator = "") { arg ->
                    if (arg.isReference) {
                        args.add(arg.referentName!!)
                        "%s"
                    } else arg.text!!
                }
                viewModel.onCompletion(value, args)
                dismiss()
            }
        }
        observe(viewModel.onArgChanged) {
            adapter.notifyItemChanged(it)
        }
    }

    fun init(
        title: CharSequence?,
        initial: Applet,
        argDescriptor: ArgumentDescriptor,
        doOnCompleted: (value: String, referents: List<String>) -> Unit
    ) = doWhenCreated {
        viewModel.title = title
        viewModel.argDescriptor = argDescriptor
        viewModel.setInitialApplet(initial)
        viewModel.onCompletion = doOnCompleted
    }
}