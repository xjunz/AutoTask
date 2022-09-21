/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.R
import top.xjunz.tasker.util.ReflectionUtil.superClassFirstParameterizedType

/**
 * @author xjunz 2022/04/24
 */
abstract class BaseBottomSheetDialog<T : ViewBinding> : BottomSheetDialogFragment(),
    HasDefaultViewModelProviderFactory {

    protected lateinit var binding: T

    open val bindingRequiredSuperClassDepth = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.BottomSheetDialogStyle)
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
        ).invoke(null, layoutInflater, container, false)!!.unsafeCast()
        return binding.root
    }

    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<*>

    @SuppressLint("RestrictedApi", "VisibleForTests")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBehaviour = (dialog as BottomSheetDialog).behavior
        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )!!
        bottomSheet.doOnPreDraw {
            val bg = bottomSheet.background as? MaterialShapeDrawable
            // keep full corner all the time
            if (bg != null && bottomSheet.height != ((bottomSheet.parent) as View).height) {
                bottomSheetBehaviour.disableShapeAnimations()
                bg.interpolation = 1F
            }
        }
        // always expand the dialog the first time being shown
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun getDefaultViewModelProviderFactory() = InnerViewModelFactory

}