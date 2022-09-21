// IAutomatorConnection.aidl
package top.xjunz.tasker;

import top.xjunz.tasker.OnCheckResultListener;
import top.xjunz.tasker.impl.IAvailabilityChecker;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import java.util.List;

interface IAutomatorConnection {

    void connect() = 1;

    long getStartTimestamp() = 2;

    boolean isConnected() = 4;

    void initAutomatorContext(in Point realSize, in Point size, float density, int scaledMinimumFlingVelocity) = 5;

    IAvailabilityChecker createAvailabilityChecker() = 6;

    oneway void destroy() = 16777114; // Destroy method defined by Shizuku server

}