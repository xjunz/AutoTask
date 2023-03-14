// IRemoteTaskManager.aidl
package top.xjunz.tasker.task.runtime;
import top.xjunz.tasker.engine.dto.XTaskDTO;
import top.xjunz.tasker.engine.task.TaskSnapshot;

interface IRemoteTaskManager {

    void initialize(in List<XTaskDTO> carriers);

    boolean isInitialized();

    void updateTask(long previous, in XTaskDTO updated);

    void removeTask(long identifier);

    void enableResidentTask(in XTaskDTO carrier);

    void addOneshotTaskIfAbsent(in XTaskDTO carrier);

    int getSnapshotCount(long identifier);

    void clearSnapshots(long identifier);

    void clearLog(long checksum, String snapshotId);

    TaskSnapshot[] getAllSnapshots(long task);
}