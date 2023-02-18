// IRemoteAutomatorService.aidl
package top.xjunz.tasker.service;

import top.xjunz.tasker.service.IAvailabilityChecker;
import top.xjunz.tasker.task.runtime.IRemoteTaskManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SharedMemory;
import java.util.List;

interface IRemoteAutomatorService {

    void connect() = 1;

    long getStartTimestamp() = 2;

    boolean isConnected() = 4;

    IAvailabilityChecker createAvailabilityChecker() = 6;

    void executeShellCmd(String cmd) = 7;

    IRemoteTaskManager getTaskManager() = 8;

    void setSystemTypefaceSharedMemory(in SharedMemory mem) = 9;

    void suppressResidentTaskScheduler(boolean suppress) = 10;

    oneway void destroy() = 16777114; // Destroy method defined by Shizuku server

}