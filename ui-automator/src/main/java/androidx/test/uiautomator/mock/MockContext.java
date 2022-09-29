package androidx.test.uiautomator.mock;

import android.util.DisplayMetrics;

/**
 * @author xjunz 2022/07/18
 */
public interface MockContext {

    /**
     * As per PowerManager.isInteractive()
     */
    boolean isInteractive();

    /**
     * Flatmap from PackageManager
     */
    String getLauncherPackageName();

    /**
     * As per WindowManager.getDefaultDisplay()
     */
    MockDisplay getDefaultDisplay();

    /**
     * As per Resource.getDisplayMetrics()
     */
    DisplayMetrics getDisplayMetrics();
}
