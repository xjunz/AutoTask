package androidx.test.uiautomator.mock;

import android.view.ViewConfiguration;

/**
 * @author xjunz 2022/07/18
 */
public class MockViewConfiguration {

    private final int mScaledMinimumFlingVelocity;

    public MockViewConfiguration(int scaledMinimumFlingVelocity) {
        mScaledMinimumFlingVelocity = scaledMinimumFlingVelocity;
    }

    public int getLongPressTimeout() {
        return ViewConfiguration.getLongPressTimeout();
    }

    public int getScaledMinimumFlingVelocity() {
        return mScaledMinimumFlingVelocity;
    }
}
