/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

import android.graphics.Point;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A {@link PointerGesture} represents the actions of a single pointer when performing a gesture.
 */
public class PointerGesture {
    // The list of actions that make up this gesture.
    private final Deque<PointerAction> mActions = new ArrayDeque<>();
    private final long mDelay;
    private long mDuration;

    /**
     * Constructs a PointerGesture which touches down at the given start point.
     */
    public PointerGesture(Point startPoint) {
        this(startPoint, 0);
    }

    /**
     * Constructs a PointerGesture which touches down at the given start point after a given delay.
     * Used in multi-point gestures when the pointers do not all touch down at the same time.
     */
    public PointerGesture(Point startPoint, long initialDelay) {
        if (initialDelay < 0) {
            throw new IllegalArgumentException("initialDelay cannot be negative");
        }
        mActions.addFirst(new PointerPauseAction(startPoint, 0));
        mDelay = initialDelay;
    }

    /**
     * Adds an action which pauses for the specified amount of {@code time} in milliseconds.
     */
    public PointerGesture pause(long time) {
        if (time < 0) {
            throw new IllegalArgumentException("time cannot be negative");
        }
        mActions.addLast(new PointerPauseAction(mActions.peekLast().end, time));
        mDuration += (mActions.peekLast().duration);
        return this;
    }

    /**
     * Adds an action that moves the pointer to {@code dest} at {@code speed} pixels per second.
     */
    public PointerGesture moveAtSpeed(Point dest, int speed) {
        mActions.addLast(new PointerLinearMoveAction(mActions.peekLast().end, dest, speed));
        mDuration += (mActions.peekLast().duration);
        return this;
    }

    public PointerGesture moveWithDuration(Point dest, long duration) {
        mActions.addLast(new PointerLinearMoveAction(mActions.peekLast().end, dest, duration));
        mDuration += (mActions.peekLast().duration);
        return this;
    }

    /**
     * Returns the start point of this gesture.
     */
    public Point start() {
        return mActions.peekFirst().start;
    }

    /**
     * Returns the end point of this gesture.
     */
    public Point end() {
        return mActions.peekLast().end;
    }

    /**
     * Returns the duration of this gesture.
     */
    public long duration() {
        return mDuration;
    }

    /**
     * Returns the amount of delay before this gesture starts.
     */
    public long delay() {
        return mDelay;
    }

    /**
     * Returns the pointer location at {@code time} milliseconds into this gesture.
     */
    public Point pointAt(long time) {
        if (time < 0) {
            throw new IllegalArgumentException("Time cannot be negative");
        }
        time -= mDelay;
        for (PointerAction action : mActions) {
            if (time < action.duration) {
                return action.interpolate((float) time / action.duration);
            }
            time -= action.duration;
        }
        return mActions.peekLast().end;
    }

    public Iterable<PointerAction> getActions() {
        return mActions;
    }

    /**
     * A {@link PointerAction} represents part of a {@link PointerGesture}.
     */
    public static abstract class PointerAction {
        public final Point start;
        public final Point end;
        public final long duration;

        public PointerAction(Point startPoint, Point endPoint, long time) {
            start = startPoint;
            end = endPoint;
            duration = time;
        }

        public abstract Point interpolate(float fraction);
    }

    /**
     * A {@link PointerPauseAction} holds the pointer steady for the given amount of time.
     */
    private static class PointerPauseAction extends PointerAction {

        public PointerPauseAction(Point startPoint, long time) {
            super(startPoint, startPoint, time);
        }

        @Override
        public Point interpolate(float fraction) {
            return new Point(start);
        }
    }

    /**
     * A {@link PointerLinearMoveAction} moves the pointer between two points at a constant
     * speed.
     */
    private static class PointerLinearMoveAction extends PointerAction {

        public PointerLinearMoveAction(Point startPoint, Point endPoint, int speed) {
            super(startPoint, endPoint, (long) (1000 * calcDistance(startPoint, endPoint) / speed));
        }

        public PointerLinearMoveAction(Point startPoint, Point endPoint, long duration) {
            super(startPoint, endPoint, duration);
        }

        @Override
        public Point interpolate(float fraction) {
            Point ret = new Point(start);
            ret.offset((int) (fraction * (end.x - start.x)), (int) (fraction * (end.y - start.y)));
            return ret;
        }

        private static double calcDistance(final Point a, final Point b) {
            return Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y));
        }
    }
}
