// IUiAutomationConnection.aidl
package android.app;

import android.accessibilityservice.IAccessibilityServiceClient;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.InputEvent;
import android.os.ParcelFileDescriptor;

/**
 * This interface contains privileged operations a shell program can perform
 * on behalf of an instrumentation that it runs. These operations require
 * special permissions which the shell user has but the instrumentation does
 * not. Running privileged operations by the shell user on behalf of an
 * instrumentation is needed for running UiTestCases.
 *
 */
interface IUiAutomationConnection {
    void connect(IAccessibilityServiceClient client, int flags);
    void disconnect();
    boolean injectInputEvent(in InputEvent event, boolean sync);
    void syncInputTransactions();
    boolean setRotation(int rotation);
    Bitmap takeScreenshot(in Rect crop, int rotation);
    void executeShellCommand(String command, in ParcelFileDescriptor sink,
            in ParcelFileDescriptor source);
    // Called from the system process.
    oneway void shutdown();
}
