package androidx.test.uiautomator.mock;

import android.app.UiAutomation;

import androidx.test.uiautomator.GestureController;
import androidx.test.uiautomator.InteractionController;
import androidx.test.uiautomator.UiDevice;

/**
 * @author xjunz 2022/07/18
 */
public interface MockInstrumentation {

    UiAutomation getUiAutomation();

    UiAutomation getUiAutomation(int flags);

    MockContext getContext();

    InteractionController getInteractionController();

    GestureController getGestureController(UiDevice device);

    MockViewConfiguration getViewConfiguration();
}
