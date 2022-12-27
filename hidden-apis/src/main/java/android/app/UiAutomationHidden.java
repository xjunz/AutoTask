/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package android.app;

import android.os.Looper;

import dev.rikka.tools.refine.RefineAs;

/**
 * @author xjunz 2021/6/23 0:48
 */
@SuppressWarnings("unused")
@RefineAs(UiAutomation.class)
public class UiAutomationHidden {

    public UiAutomationHidden(Looper looper, IUiAutomationConnection connection) {
        throw new RuntimeException("Stub!");
    }

    public void connect() {
        throw new RuntimeException("Stub!");
    }

    public void connect(int flag) {
        throw new RuntimeException("Stub!");
    }

    public void disconnect() {
        throw new RuntimeException("Stub!");
    }
}
