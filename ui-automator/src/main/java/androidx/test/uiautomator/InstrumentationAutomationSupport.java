/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

import android.app.Instrumentation;
import android.os.Bundle;

/**
 * A wrapper around {@link Instrumentation} to provide sendStatus function
 *
 * Provided for backwards compatibility purpose. New code should use
 * {@link Instrumentation#sendStatus(int, Bundle)} instead.
 *
 */
class InstrumentationAutomationSupport implements IAutomationSupport {

    private Instrumentation mInstrumentation;

    InstrumentationAutomationSupport(Instrumentation instrumentation) {
        mInstrumentation = instrumentation;
    }

    @Override
    public void sendStatus(int resultCode, Bundle status) {
        mInstrumentation.sendStatus(resultCode, status);
    }
}
