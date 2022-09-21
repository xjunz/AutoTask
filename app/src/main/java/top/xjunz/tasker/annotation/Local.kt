/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.annotation

/**
 * The annotation indicating that its targets are only used in the local process. Only use this inside
 * [LocalAndRemote] annotation devoted classes.
 *
 * @author xjunz 2022/05/25
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Local
