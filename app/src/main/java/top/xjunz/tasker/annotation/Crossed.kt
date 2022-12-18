/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.annotation

/**
 * The annotation indicating that its target may be used in both local and remote processes.
 * You should deal with these two situations carefully.
 *
 * @see RemoteOnly
 *
 * @author xjunz 2022/04/30
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Crossed