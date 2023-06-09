/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.DialogCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.util.ReflectionUtil.superClassFirstParameterizedType

/**
 * @author xjunz 2022/04/24
 */
abstract class BaseBottomSheetDialog<T : ViewBinding> : BottomSheetDialogFragment(),
    HasDefaultViewModelProviderFactory {

    protected lateinit var binding: T

    protected open val bindingRequiredSuperClassDepth = 1

    private val mixin by lazy {
        DialogStackMixin(this, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        var superClass: Class<*> = javaClass
        for (i in 1 until bindingRequiredSuperClassDepth) {
            superClass = superClass.superclass
        }
        binding = superClass.superClassFirstParameterizedType().getDeclaredMethod(
            "inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java
        ).invoke(null, layoutInflater, container, false)!!.casted()
        return binding.root
    }

    @SuppressLint("RestrictedApi", "VisibleForTests")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val behaviour = (dialog as BottomSheetDialog).behavior
        behaviour.skipCollapsed = true
        val bottomSheet = DialogCompat.requireViewById(
            dialog!!, com.google.android.material.R.id.design_bottom_sheet
        )
        bottomSheet.doOnPreDraw {
            val bg = bottomSheet.background as? MaterialShapeDrawable
            // keep full corner all the time if not full screen
            if (bg != null && bottomSheet.height != ((bottomSheet.parent) as View).height) {
                behaviour.disableShapeAnimations()
                bg.interpolation = 1F
            }
        }
        // always expand the dialog the first time being shown
        behaviour.state = BottomSheetBehavior.STATE_EXPANDED
        mixin.doOnViewCreated()
    }

    override fun onStart() {
        super.onStart()
        mixin.doOnStart()
    }

    override fun dismiss() {
        mixin.doOnDismissOrCancel()
        super.dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        mixin.doOnDismissOrCancel()
        super.onCancel(dialog)
    }

    override val defaultViewModelProviderFactory = InnerViewModelFactory

}