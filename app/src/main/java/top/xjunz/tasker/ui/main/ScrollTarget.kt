/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import androidx.recyclerview.widget.RecyclerView

/**
 * @author xjunz 2023/03/05
 */
interface ScrollTarget {
    fun getScrollTarget(): RecyclerView?
}