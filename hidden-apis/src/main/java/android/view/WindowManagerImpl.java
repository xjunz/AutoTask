/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package android.view;

import android.os.IBinder;

/**
 * @author xjunz 2023/01/03
 */
public abstract class WindowManagerImpl implements WindowManager {

    public void setDefaultToken(IBinder binder) {
        throw new RuntimeException("Stub!");
    }
}
