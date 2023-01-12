// IRemoteTaskManager.aidl
package top.xjunz.tasker.task.runtime;
import top.xjunz.tasker.engine.dto.XTaskDTO;

interface IRemoteTaskManager {

    void initialize(in List<XTaskDTO> carriers);

    boolean isInitialized();

    void updateResidentTask(long previous, in XTaskDTO updated);

    void disableResidentTask(long identifier);

    void enableResidentTask(in XTaskDTO carrier);

}