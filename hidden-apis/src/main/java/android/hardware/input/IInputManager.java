package android.hardware.input;

import android.os.IBinder;
import android.view.InputEvent;

/**
 * @author xjunz 2021/6/22
 */
@SuppressWarnings("unused")
public interface IInputManager {

    boolean injectInputEvent(InputEvent event, int mode);

    abstract class Stub{

        public static IInputManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
