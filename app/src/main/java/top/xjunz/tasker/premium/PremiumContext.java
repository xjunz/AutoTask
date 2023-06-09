/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.premium;

/**
 * @author xjunz 2023/03/01
 */
public class PremiumContext {

    public static final String placeholder = "placeholder";

    @FieldOrdinal(ordinal = 1)
    public String orderId = placeholder;

    @FieldOrdinal(ordinal = 2)
    public String createTimestamp = placeholder;

    @FieldOrdinal(ordinal = 20)
    public String empty = placeholder;

    @FieldOrdinal(ordinal = 30)
    public String delimiter = placeholder;

    @FieldOrdinal(ordinal = 40)
    public String screenShotAction;

    @FieldOrdinal(ordinal = 50)
    public String checksum;

}
