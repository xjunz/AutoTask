// IRemoteTaskScheduler.aidl
package top.xjunz.tasker.task.runtime;

import top.xjunz.tasker.engine.dto.XTaskDTO;
import top.xjunz.tasker.task.runtime.ITaskCompletionCallback;

interface IRemoteTaskScheduler {

    void scheduleTask(in XTaskDTO dto, in ITaskCompletionCallback callback);

}