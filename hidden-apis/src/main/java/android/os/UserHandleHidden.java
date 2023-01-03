/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package android.os;

import dev.rikka.tools.refine.RefineAs;

/**
 * @author xjunz 2023/01/03
 */
@RefineAs(UserHandle.class)
public class UserHandleHidden {

    public static UserHandle of(int uid) {
        throw new RuntimeException("Stub!");
    }
}
