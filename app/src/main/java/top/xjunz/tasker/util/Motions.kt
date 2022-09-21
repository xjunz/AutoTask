package top.xjunz.tasker.util

import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.AttrRes
import com.google.android.material.R
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.motion.MotionUtils
import top.xjunz.tasker.app

object Motions {

    @SuppressLint("RestrictedApi")
    private fun Context.resolveEasingMotion(@AttrRes motionAttr: Int): TimeInterpolator {
        return MotionUtils.resolveThemeInterpolator(
            this, motionAttr, AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
        )
    }

    val EASING_EMPHASIZED by lazy {
        app.resolveEasingMotion(R.attr.motionEasingEmphasized)
    }
}