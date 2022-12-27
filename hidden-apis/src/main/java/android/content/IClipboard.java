/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package android.content;

/**
 * @author xjunz 2022/11/16
 */
public interface IClipboard {

    void setPrimaryClip(android.content.ClipData clip, java.lang.String callingPackage, int userId);

    abstract class Stub {

        public static android.content.IClipboard asInterface(android.os.IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
