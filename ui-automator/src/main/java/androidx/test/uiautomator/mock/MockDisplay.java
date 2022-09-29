package androidx.test.uiautomator.mock;

import android.graphics.Point;
import android.util.DisplayMetrics;

/**
 * @author xjunz 2022/07/18
 */
public interface MockDisplay {

    void getRealSize(Point p);

    DisplayMetrics getRealMetrics();

    void getSize(Point p);

    int getRotation();
}
