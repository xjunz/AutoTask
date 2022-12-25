// IRemoteTaskManager.aidl
package top.xjunz.tasker.task.runtime;
import top.xjunz.tasker.engine.task.dto.XTaskDTO;

interface IRemoteTaskManager {

    void initialize(in List<XTaskDTO> carriers);

    boolean isInitialized();

    void enableCachedResidentTask(long identifier);

    void removeResidentTask(long identifier);

    void enableNewResidentTask(in XTaskDTO carrier);

    void removeCachedTask(long checksum);

    boolean isTaskCached(long identifier);

}