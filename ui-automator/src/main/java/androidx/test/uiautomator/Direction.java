/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

/**
 * An enumeration used to specify the primary direction of certain gestures.
 */
public enum Direction {
    LEFT, RIGHT, UP, DOWN;

    private Direction mOpposite;

    static {
        LEFT.mOpposite = RIGHT;
        RIGHT.mOpposite = LEFT;
        UP.mOpposite = DOWN;
        DOWN.mOpposite = UP;
    }

    public static final Direction[] ALL_DIRECTIONS = {
            Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.DOWN};

    /**
     * Returns the reverse of the given direction.
     */
    public static Direction reverse(Direction direction) {
        return direction.mOpposite;
    }
}
