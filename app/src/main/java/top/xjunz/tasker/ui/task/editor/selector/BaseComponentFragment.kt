package top.xjunz.tasker.ui.task.editor.selector

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.databinding.FragmentComponentSelectorBinding
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ui.base.BaseFragment

/**
 * @author xjunz 2022/10/09
 */
abstract class BaseComponentFragment : BaseFragment<FragmentComponentSelectorBinding>() {

    override val bindingRequiredSuperClassDepth: Int = 2

    protected lateinit var viewModel: ComponentSelectorViewModel

    protected val parentFragment: ComponentSelectorDialog get() = requireParentFragment().casted()

    abstract val index: Int

    abstract fun findItem(item: Any): Int

    private fun notifyItemChanged(item: Any) {
        val index = findItem(item)
        if (index >= 0) binding.rvList.adapter?.notifyItemChanged(index, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = requireParentFragment().viewModels<ComponentSelectorViewModel>().value
        binding.rvList.id = View.generateViewId()
        observe(viewModel.currentItem) {
            if (it == index) {
                parentFragment.appBar.liftOnScrollTargetViewId = binding.rvList.id
                parentFragment.appBar.isLifted =
                    (binding.rvList.canScrollVertically(-1) || binding.rvList.scrollY > 0)
            }
        }
        observeTransient(viewModel.onSelectionCleared) {
            binding.rvList.adapter?.notifyItemRangeChanged(
                0, binding.rvList.adapter!!.itemCount, true
            )
        }
        observeTransient(viewModel.addedItem) {
            notifyItemChanged(it)
        }
        observeTransient(viewModel.removedItem) {
            notifyItemChanged(it)
        }
        observe(viewModel.appBarHeight) {
            binding.rvList.updatePadding(top = it)
        }
    }
}