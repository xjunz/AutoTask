/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

/**
 * Generated in test runs when a {@link UiSelector} selector could not be matched
 * to any UI element displayed.
 * @since API Level 16
 */
public class UiObjectNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @since API Level 16
     **/
    public UiObjectNotFoundException(String msg) {
        super(msg);
    }

    /**
     * @since API Level 16
     **/
    public UiObjectNotFoundException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * @since API Level 16
     **/
    public UiObjectNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
