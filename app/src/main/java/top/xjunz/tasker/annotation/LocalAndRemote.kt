/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.annotation

import java.lang.annotation.Inherited

/**
 * The annotation indicating that its target may be used in both local and remote processes.
 * You should deal with these two situations carefully.
 *
 * @see RemoteOnly
 *
 * @author xjunz 2022/04/30
 */
@Inherited
@Retention(AnnotationRetention.SOURCE)
annotation class LocalAndRemote