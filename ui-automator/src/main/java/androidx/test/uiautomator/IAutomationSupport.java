/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

import android.os.Bundle;

/**
 * Provides auxiliary support for running test cases
 *
 * @since API Level 16
 */
public interface IAutomationSupport {

    /**
     * Allows the running test cases to send out interim status
     *
     * @param resultCode
     * @param status status report, consisting of key value pairs
     * @since API Level 16
     */
    public void sendStatus(int resultCode, Bundle status);

}
