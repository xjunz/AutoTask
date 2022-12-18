// IRemoteTaskManager.aidl
package top.xjunz.tasker.engine.runtime;
import top.xjunz.tasker.engine.task.dto.XTaskDTO;

interface IRemoteTaskManager {

    void enableResidentTask(long id);

    void disableResidentTask(long id);

    void addNewEnabledResidentTask(in XTaskDTO task);

}