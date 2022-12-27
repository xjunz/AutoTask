/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

import android.view.accessibility.AccessibilityEvent;

/**
 * An {@link EventCondition} is a condition which depends on an event or series of events having
 * occurred.
 */
public abstract class EventCondition<R> extends Condition<AccessibilityEvent, Boolean> {
    abstract Boolean apply(AccessibilityEvent event);

    abstract R getResult();
}
