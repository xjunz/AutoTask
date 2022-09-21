package android.content.pm;

import android.content.ComponentName;
import android.os.IBinder;
import android.os.IInterface;

import java.util.List;

/**
 * @author xjunz 2021/7/8
 */
public interface IPackageManager extends IInterface {
    ComponentName getHomeActivities(List<ResolveInfo> outHomeCandidates);

    /**
     * As per {@link android.content.pm.PackageManager#setComponentEnabledSetting}.
     */
    void setComponentEnabledSetting(ComponentName componentName,
                                    int newState, int flags, int userId);

    ApplicationInfo getApplicationInfo(String packageName, long flags, int userId);

    abstract class Stub {

        public static IPackageManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
