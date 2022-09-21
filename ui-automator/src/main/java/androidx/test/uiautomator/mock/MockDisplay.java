package androidx.test.uiautomator.mock;

import android.graphics.Point;

/**
 * @author xjunz 2022/07/18
 */
public interface MockDisplay {

    void getRealSize(Point p);

    MockDisplayMetrics getRealMetrics();

    void getSize(Point p);

    int getRotation();
}
