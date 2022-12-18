// IRemoteAutomatorService.aidl
package top.xjunz.tasker.service;

import top.xjunz.tasker.service.IAvailabilityChecker;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import java.util.List;

interface IRemoteAutomatorService {

    void connect() = 1;

    long getStartTimestamp() = 2;

    boolean isConnected() = 4;

    IAvailabilityChecker createAvailabilityChecker() = 6;

    void executeShellCmd(String cmd) = 7;

    oneway void destroy() = 16777114; // Destroy method defined by Shizuku server

}