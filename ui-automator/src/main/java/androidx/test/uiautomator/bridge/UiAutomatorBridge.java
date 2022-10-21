package androidx.test.uiautomator.bridge;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;

import androidx.test.uiautomator.GestureController;
import androidx.test.uiautomator.InteractionController;
import androidx.test.uiautomator.UiDevice;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @author xjunz 2022/09/30
 */
public abstract class UiAutomatorBridge {

    private static final String LOG_TAG = UiAutomatorBridge.class.getSimpleName();

    /**
     * This value has the greatest bearing on the appearance of test execution speeds.
     * This value is used as the minimum time to wait before considering the UI idle after
     * each action.
     */
    private static final long QUIET_TIME_TO_BE_CONSIDERD_IDLE_STATE = 500;//ms

    /**
     * This is the maximum time the automation will wait for the UI to go idle. Execution
     * will resume normally anyway. This is to prevent waiting forever on display updates
     * that may be related to spinning wheels or progress updates of sorts etc...
     */
    private static final long TOTAL_TIME_TO_WAIT_FOR_IDLE_STATE = 1000 * 10;//ms

    private final UiAutomation mUiAutomation;

    private DisplayMetrics mDefaultDisplayMetrics;

    private final List<UiAutomation.OnAccessibilityEventListener> mEventListeners = new ArrayList<>(2);

    private final UiAutomation.OnAccessibilityEventListener mEventListener = event -> {
        synchronized (mEventListeners) {
            for (UiAutomation.OnAccessibilityEventListener listener : mEventListeners) {
                listener.onAccessibilityEvent(event);
            }
        }
    };

    public void startReceivingEvents() {
        mUiAutomation.setOnAccessibilityEventListener(mEventListener);
    }

    public void stopReceivingEvents() {
        mUiAutomation.setOnAccessibilityEventListener(null);
    }

    public UiAutomatorBridge(UiAutomation uiAutomation) {
        mUiAutomation = uiAutomation;
    }

    public UiAutomation getUiAutomation() {
        return mUiAutomation;
    }

    public abstract InteractionController getInteractionController();

    /**
     * Shizuku only
     */
    public boolean injectInputEvent(InputEvent event, boolean sync) {
        return mUiAutomation.injectInputEvent(event, sync);
    }

    /**
     * Shizuku only
     */
    public boolean setRotation(int rotation) {
        return mUiAutomation.setRotation(rotation);
    }

    public void setCompressedLayoutHierarchy(boolean compressed) {
        AccessibilityServiceInfo info = mUiAutomation.getServiceInfo();
        if (compressed)
            info.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        else
            info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        mUiAutomation.setServiceInfo(info);
    }

    public abstract int getRotation();

    public abstract boolean isScreenOn();

    public void waitForIdle() {
        waitForIdle(TOTAL_TIME_TO_WAIT_FOR_IDLE_STATE);
    }

    public void waitForIdle(long timeout) {
        try {
            mUiAutomation.waitForIdle(QUIET_TIME_TO_BE_CONSIDERD_IDLE_STATE, timeout);
        } catch (TimeoutException te) {
            Log.w(LOG_TAG, "Could not detect idle state.", te);
        }
    }

    public void addOnAccessibilityEventListener(UiAutomation.OnAccessibilityEventListener listener) {
        synchronized (mEventListeners) {
            mEventListeners.add(listener);
        }
    }

    public boolean takeScreenshot(File storePath, int quality) {
        Bitmap screenshot = mUiAutomation.takeScreenshot();
        if (screenshot == null) {
            return false;
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(storePath))) {
            screenshot.compress(Bitmap.CompressFormat.PNG, quality, bos);
            bos.flush();
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "failed to save screen shot to file", ioe);
            return false;
        } finally {
            /* ignore */
            screenshot.recycle();
        }
        return true;
    }

    public abstract Display getDefaultDisplay();

    public abstract String getLauncherPackageName();

    public abstract int getScaledMinimumFlingVelocity();

    /**
     * Only for density purpose, may not accurate for screen width and screen height.
     */
    public DisplayMetrics getDisplayMetrics() {
        if (mDefaultDisplayMetrics == null) {
            mDefaultDisplayMetrics = new DisplayMetrics();
            getDefaultDisplay().getMetrics(mDefaultDisplayMetrics);
        }
        return mDefaultDisplayMetrics;
    }


    public abstract GestureController getGestureController(UiDevice device);
}
