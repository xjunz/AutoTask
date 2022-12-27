/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

/**
 * A {@link StaleObjectException} exception is thrown when a {@link UiObject2} is used after the
 * underlying {@link android.view.View} has been destroyed. In this case, it is necessary to call
 * {@link UiDevice#findObject(BySelector)} to obtain a new {@link UiObject2} instance.
 */
public class StaleObjectException extends RuntimeException {
}
