package android.os;

import dev.rikka.tools.refine.RefineAs;

/**
 * @author xjunz 2022/12/24
 */
@RefineAs(IBinder.class)
public interface IBinderHidden {
    int SHELL_COMMAND_TRANSACTION = ('_'<<24)|('C'<<16)|('M'<<8)|'D';
}
