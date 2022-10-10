/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.annotation

/**
 * The annotation indicating that its targets are only used in the remote process. Only use this inside
 * [LocalAndRemote] annotation devoted classes.
 *
 * @author xjunz 2022/04/29
 */
@Retention(AnnotationRetention.SOURCE)
annotation class RemoteOnly
