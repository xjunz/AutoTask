/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package x;

/**
 * @author xjunz 2023/03/01
 */
public class f /* Security */ {

    static {
        System.loadLibrary("ssl");
    }

    public static native String alpha(byte[] bytes); // encrypt

    public static native byte[] delta(String str); // decrypt
}
