/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.content.Context
import androidx.annotation.AttrRes
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * @author xjunz 2022/07/28
 */

fun Context.createMaterialShapeDrawable(
    @AttrRes fillColorRes: Int = com.google.android.material.R.attr.colorSurface,
    elevation: Float = 3.dpFloat, cornerSize: Float = 16.dpFloat
): MaterialShapeDrawable {
    return MaterialShapeDrawable.createWithElevationOverlay(this, elevation).apply {
        shapeAppearanceModel = ShapeAppearanceModel.builder().setAllCornerSizes(cornerSize).build()
        fillColor = fillColorRes.attrColorStateList
    }
}