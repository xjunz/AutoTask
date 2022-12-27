/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

import android.os.SystemClock;

/**
 * Mixin which provides functionality to wait for conditions that depend on a given object.
 */
class WaitMixin<T> {

    private static final long DEFAULT_POLL_INTERVAL = 1000;
    private T mObject;

    public WaitMixin(T instance) {
        mObject = instance;
    }

    public <R> R wait(Condition<? super T, R> condition, long timeout) {
        return wait(condition, timeout, DEFAULT_POLL_INTERVAL);
    }

    public <R> R wait(Condition<? super T, R> condition, long timeout, long interval) {
        long startTime = SystemClock.uptimeMillis();

        R result = condition.apply(mObject);
        for (long elapsedTime = 0; result == null || result.equals(false);
                elapsedTime = SystemClock.uptimeMillis() - startTime) {

            if (elapsedTime >= timeout) {
                break;
            }

            SystemClock.sleep(interval);
            result = condition.apply(mObject);
        }
        return result;
    }
}
