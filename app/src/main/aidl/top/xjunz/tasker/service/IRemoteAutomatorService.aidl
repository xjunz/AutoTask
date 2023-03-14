// IRemoteAutomatorService.aidl
package top.xjunz.tasker.service;

import top.xjunz.tasker.task.runtime.IRemoteTaskManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SharedMemory;
import java.util.List;
import top.xjunz.tasker.engine.dto.XTaskDTO;
import top.xjunz.tasker.task.runtime.ITaskCompletionCallback;
import android.os.ResultReceiver;

interface IRemoteAutomatorService {

    void connect(in ResultReceiver callback) = 1;

    long getStartTimestamp() = 2;

    boolean isConnected() = 4;

    IRemoteTaskManager getTaskManager() = 8;

    void setSystemTypefaceSharedMemory(in SharedMemory mem) = 9;

    void suppressResidentTaskScheduler(boolean suppress) = 10;

    void scheduleOneshotTask(long id, in ITaskCompletionCallback callback) = 11;

    void stopOneshotTask(long id) = 12;

    void setPremiumContextStoragePath(in String path) = 13;

    void loadPremiumContext() = 14;

    oneway void destroy() = 16777114; // Destroy method defined by Shizuku server

}