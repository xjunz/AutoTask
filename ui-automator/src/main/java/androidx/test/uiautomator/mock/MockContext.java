package androidx.test.uiautomator.mock;

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
    MockDisplayMetrics getDisplayMetrics();
}
