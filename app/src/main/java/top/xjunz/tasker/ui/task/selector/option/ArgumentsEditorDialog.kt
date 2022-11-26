package top.xjunz.tasker.ui.task.selector.option

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogArgumentsEditorBinding
import top.xjunz.tasker.databinding.ItemArgumentEditorBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.ValueDescriptor
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.ui.task.editor.GlobalFlowEditorViewModel
import top.xjunz.tasker.util.Router.launchAction

/**
 * @author xjunz 2022/11/22
 */
class ArgumentsEditorDialog : BaseDialogFragment<DialogArgumentsEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        lateinit var option: AppletOption

        lateinit var applet: Applet

        lateinit var doOnCompletion: () -> Unit

        val changedRefidSet = mutableSetOf<String>()

        val onItemChanged = MutableLiveData<ValueDescriptor>()

        private var revocations = mutableMapOf<ValueDescriptor, Runnable>()

        /**
         * Revoke operations related to this [arg].
         */
        fun revoke(arg: ValueDescriptor) {
            revocations[arg]?.run()
            revocations.remove(arg)
        }

        fun registerRevocationIfAbsent(arg: ValueDescriptor, block: Runnable) {
            if (revocations.containsKey(arg))
                return
            revocations[arg] = block
        }

        fun revokeAll() {
            revocations.forEach {
                it.value.run()
            }
            clearRevocations()
        }

        fun clearRevocations() = revocations.clear()

        fun checkForUnspecifiedArgument(): Int {
            option.arguments.forEachIndexed { which, arg ->
                val isValueSet = applet.value != null
                if (arg.isValueOnly && !isValueSet) {
                    return which
                }
                val isReferenceSet = applet.referring[which] != null
                if (arg.isReferenceOnly && !isReferenceSet) {
                    return which
                }
                if (arg.isTolerant && !isValueSet && !isReferenceSet) {
                    return which
                }
            }
            return -1
        }
    }

    private inline val option get() = viewModel.option

    private inline val applet get() = viewModel.applet

    private val viewModel by viewModels<InnerViewModel>()

    private val globalViewModel by activityViewModels<GlobalFlowEditorViewModel>()

    private fun showInputDialog(which: Int, arg: ValueDescriptor) {
        TextEditorDialog().configEditText { et ->
            et.configInputType(arg.type, true)
            et.maxLines = 5
        }.setCaption(option.helpText).setArguments(arg.name, applet.value?.toString()) set@{
            if (it.isEmpty()) return@set R.string.error_empty_input.str
            val parsed = arg.parseValueFromInput(it) ?: return@set R.string.error_mal_format.str
            viewModel.revoke(arg)
            val refid = applet.referring[which]
            val prevValue = applet.value
            applet.removeReference(which)
            applet.value = parsed
            viewModel.registerRevocationIfAbsent(arg) {
                applet.value = prevValue
                if (refid != null) applet.setReference(which, refid)
            }
            viewModel.onItemChanged.value = arg
            return@set null
        }.show(parentFragmentManager)
    }

    private fun showReferenceSelectorDialog(whichArg: Int, arg: ValueDescriptor) {
        FlowEditorDialog().setReferenceToSelect(applet, arg).setFlow(globalViewModel.root, true)
            .doOnReferenceSelected { refApplet, whichRefid, refid ->
                viewModel.revoke(arg)
                val prevRefid = applet.referring[whichArg]
                refApplet.setRefid(whichRefid, refid)
                applet.setReference(whichArg, refid)
                val value = applet.value
                if (!arg.isReferenceOnly) {
                    applet.value = null
                }
                viewModel.registerRevocationIfAbsent(arg) {
                    refApplet.setRefid(whichRefid, prevRefid)
                    applet.setReference(whichArg, prevRefid)
                    applet.value = value
                }
                viewModel.onItemChanged.value = arg
            }.show(childFragmentManager)
    }

    private val adapter by lazy {
        inlineAdapter(option.arguments, ItemArgumentEditorBinding::class.java, {
            binding.root.setOnClickListener {
                val which = adapterPosition
                val arg = option.arguments[which]
                if (arg.isTolerant) {
                    val popup = PopupMenu(requireContext(), it, Gravity.END)
                    popup.menu.add(R.string.refer_to.format(arg.name))
                    popup.menu.add(R.string.specify_value.format(arg.name))
                    popup.show()
                    popup.setOnMenuItemClickListener set@{ item ->
                        when (popup.indexOf(item)) {
                            0 -> showReferenceSelectorDialog(which, arg)
                            1 -> showInputDialog(which, arg)
                        }
                        return@set true
                    }
                } else if (arg.isReferenceOnly) {
                    showReferenceSelectorDialog(which, arg)
                } else if (arg.isValueOnly) {
                    showInputDialog(which, arg)
                }
            }
            binding.tvValue.setOnClickListener {
                val position = adapterPosition
                val arg = option.arguments[position]
                val refid = applet.referring.getValue(position)
                val refApplet = globalViewModel.root.requireChildOwningRefid(refid)
                val refWhich = refApplet.referred.entries.first { it.value == refid }.key
                val result = globalViewModel.factory.requireOption(refApplet).results[refWhich]
                TextEditorDialog().setCaption(
                    R.string.format_set_refid.formatSpans(result.name.foreColored().clickable {
                        it.context.launchAction(TextEditorDialog.ACTION_INPUT, result.name)
                    })
                ).configEditText {
                    it.setMaxLength(Applet.Configurator.MAX_REFERENCE_ID_LENGTH)
                }.setArguments(R.string.edit_refid.text, refid) {
                    if (it.isEmpty()) {
                        return@setArguments R.string.error_empty_input.text
                    }
                    if (it != refid && globalViewModel.root.findChildOwningRefid(it) != null) {
                        return@setArguments R.string.error_tag_exists.text
                    }
                    refApplet.setRefid(refWhich, it)
                    applet.setReference(position, it)
                    globalViewModel.renameRefid(refid, it)
                    viewModel.changedRefidSet.remove(refid)
                    viewModel.changedRefidSet.add(it)
                    viewModel.registerRevocationIfAbsent(arg) {
                        refApplet.setRefid(refWhich, refid)
                        applet.setReference(position, refid)
                        globalViewModel.renameRefid(it, refid)
                    }
                    viewModel.onItemChanged.value = arg
                    return@setArguments null
                }.show(childFragmentManager)
            }
        }) { binding, pos, arg ->
            binding.tvTitle.text = arg.name
            binding.tvValue.isEnabled = true
            binding.tvValue.isClickable = false
            binding.tvValue.background = null
            if (!arg.isValueOnly && applet.referring.containsKey(pos)) {
                val refid = applet.referring.getValue(pos)
                // Not value only and the reference is available
                binding.tvValue.text = refid.underlined().foreColored()
                binding.tvValue.setDrawableStart(R.drawable.ic_baseline_link_24)
                binding.tvValue.isClickable = true
                binding.tvValue.background =
                    android.R.attr.selectableItemBackground.resolvedId.getDrawable()
            } else if (!arg.isReferenceOnly && applet.value != null) {
                // Not reference only and the value is available
                binding.tvValue.text = option.describe(applet)
                binding.tvValue.setDrawableStart(R.drawable.ic_baseline_text_fields_24)
            } else {
                binding.tvValue.text = R.string.unspecified.text.italic()
                binding.tvValue.setDrawableStart(View.NO_ID)
                binding.tvValue.isEnabled = false
            }
            if (arg.isValueOnly) {
                binding.ivEnter.setImageResource(R.drawable.ic_baseline_edit_24)
            } else {
                binding.ivEnter.setImageResource(R.drawable.ic_baseline_chevron_right_24)
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        viewModel.revokeAll()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvArgument.adapter = adapter
        binding.tvTitle.text = option.currentTitle
        binding.btnCancel.setOnClickListener {
            viewModel.revokeAll()
            dismiss()
        }
        binding.btnComplete.setOnClickListener {
            val illegal = viewModel.checkForUnspecifiedArgument()
            if (illegal == -1) {
                viewModel.clearRevocations()
                viewModel.doOnCompletion()
                viewModel.changedRefidSet.forEach {
                    globalViewModel.notifyRefidChanged(it)
                }
                dismiss()
            } else {
                val item = binding.rvArgument.findViewHolderForAdapterPosition(illegal)?.itemView
                item?.shake()
                toast(R.string.format_not_specified.format(option.arguments[illegal].name))
            }
        }
        observeTransient(viewModel.onItemChanged) {
            adapter.notifyItemChanged(option.arguments.indexOf(it), true)
        }
    }

    fun setAppletOption(applet: Applet, option: AppletOption) = doWhenCreated {
        viewModel.option = option
        viewModel.applet = applet
    }

    fun doOnCompletion(block: () -> Unit) = doWhenCreated {
        viewModel.doOnCompletion = block
    }
}