/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

import android.graphics.Point;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * The {@link GestureController} provides methods for performing high-level {@link PointerGesture}s.
 */
public class GestureController {

    public static final long MOTION_EVENT_INJECTION_DELAY_MILLIS = 5;

    // Singleton instance
    private static GestureController sInstance;

    private final UiDevice mDevice;

    /**
     * Comparator for sorting PointerGestures by start times.
     */
    private static final Comparator<PointerGesture> START_TIME_COMPARATOR =
            new Comparator<PointerGesture>() {

                @Override
                public int compare(PointerGesture o1, PointerGesture o2) {
                    return (int) (o1.delay() - o2.delay());
                }
            };

    /**
     * Comparator for sorting PointerGestures by end times.
     */
    private static final Comparator<PointerGesture> END_TIME_COMPARATOR =
            new Comparator<PointerGesture>() {

                @Override
                public int compare(PointerGesture o1, PointerGesture o2) {
                    return (int) ((o1.delay() + o2.duration()) - (o2.delay() + o2.duration()));
                }
            };


    public GestureController(UiDevice device) {
        mDevice = device;
    }

    /**
     * Returns the {@link GestureController} instance for the given {@link UiDevice}.
     */
    public static GestureController getInstance(UiDevice device) {
        if (sInstance == null || sInstance.mDevice != device) {
            sInstance = device.getBridge().getGestureController(device);
        }

        return sInstance;
    }

    /**
     * Performs the given gesture and waits for the {@code condition} to be met.
     *
     * @param condition The {@link EventCondition} to wait for.
     * @param timeout   Maximum amount of time to wait in milliseconds.
     * @param gestures  One or more {@link PointerGesture}s which define the gesture to be performed.
     * @return The final result returned by the condition.
     */
    public <R> R performGestureAndWait(EventCondition<R> condition, long timeout,
                                       PointerGesture... gestures) {

        return getDevice().performActionAndWait(new GestureRunnable(gestures), condition, timeout);
    }

    /**
     * Performs the given gesture as represented by the given {@link PointerGesture}s.
     * <p>
     * Each {@link PointerGesture} represents the actions of a single pointer from the time when it
     * is first touched down until the pointer is released. To perform the gesture, this method
     * tracks the locations of each pointer and injects {@link MotionEvent}s as appropriate.
     *
     * @param gestures One or more {@link PointerGesture}s which define the gesture to be performed.
     */
    public void performGesture(PointerGesture... gestures) {
        // Initialize pointers
        int count = 0;
        Map<PointerGesture, Pointer> pointers = new HashMap<PointerGesture, Pointer>();
        for (PointerGesture g : gestures) {
            pointers.put(g, new Pointer(count++, g.start()));
        }

        // Initialize MotionEvent arrays
        List<PointerProperties> properties = new ArrayList<PointerProperties>();
        List<PointerCoords> coordinates = new ArrayList<PointerCoords>();

        // Track active and pending gestures
        PriorityQueue<PointerGesture> active = new PriorityQueue<PointerGesture>(gestures.length,
                END_TIME_COMPARATOR);
        PriorityQueue<PointerGesture> pending = new PriorityQueue<PointerGesture>(gestures.length,
                START_TIME_COMPARATOR);
        pending.addAll(Arrays.asList(gestures));

        // Record the start time
        long startTime = SystemClock.uptimeMillis();

        // Loop
        MotionEvent event;
        for (long elapsedTime = 0; !pending.isEmpty() || !active.isEmpty();
             elapsedTime = SystemClock.uptimeMillis() - startTime) {

            // Touchdown any new pointers
            while (!pending.isEmpty() && elapsedTime > pending.peek().delay()) {
                PointerGesture gesture = pending.remove();
                Pointer pointer = pointers.get(gesture);

                // Add the pointer to the MotionEvent arrays
                properties.add(pointer.prop);
                coordinates.add(pointer.coords);

                // Touch down
                int action = MotionEvent.ACTION_DOWN;
                if (!active.isEmpty()) {
                    // Use ACTION_POINTER_DOWN for secondary pointers. The index is stored at
                    // ACTION_POINTER_INDEX_SHIFT.
                    action = MotionEvent.ACTION_POINTER_DOWN
                            + ((properties.size() - 1) << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
                }
                event = getMotionEvent(startTime, startTime + elapsedTime, action, properties,
                        coordinates);
                getDevice().getUiAutomation().injectInputEvent(event, true);

                // Move the PointerGesture to the active list
                active.add(gesture);
            }

            // Touch up any completed pointers
            while (!active.isEmpty()
                    && elapsedTime > active.peek().delay() + active.peek().duration()) {

                PointerGesture gesture = active.remove();
                Pointer pointer = pointers.get(gesture);

                // Update pointer positions
                pointer.updatePosition(gesture.end());
                for (PointerGesture current : active) {
                    pointers.get(current).updatePosition(current.pointAt(elapsedTime));
                }

                int action = MotionEvent.ACTION_UP;
                int index = properties.indexOf(pointer.prop);
                if (!active.isEmpty()) {
                    action = MotionEvent.ACTION_POINTER_UP
                            + (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
                }
                event = getMotionEvent(startTime, startTime + elapsedTime, action, properties,
                        coordinates);
                getDevice().getUiAutomation().injectInputEvent(event, true);

                properties.remove(index);
                coordinates.remove(index);
            }

            // Move any active pointers
            for (PointerGesture gesture : active) {
                Pointer pointer = pointers.get(gesture);
                pointer.updatePosition(gesture.pointAt(elapsedTime - gesture.delay()));

            }
            if (!active.isEmpty()) {
                event = getMotionEvent(startTime, startTime + elapsedTime, MotionEvent.ACTION_MOVE,
                        properties, coordinates);
                getDevice().getUiAutomation().injectInputEvent(event, true);
            }
        }
    }

    /**
     * Helper function to obtain a MotionEvent.
     */
    private static MotionEvent getMotionEvent(long downTime, long eventTime, int action,
                                              List<PointerProperties> properties, List<PointerCoords> coordinates) {

        PointerProperties[] props = properties.toArray(new PointerProperties[properties.size()]);
        PointerCoords[] coords = coordinates.toArray(new PointerCoords[coordinates.size()]);
        return MotionEvent.obtain(downTime, eventTime, action, props.length, props, coords,
                0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    /**
     * Helper class which tracks an individual pointer as part of a MotionEvent.
     */
    private static class Pointer {
        PointerProperties prop;
        PointerCoords coords;

        public Pointer(int id, Point point) {
            prop = new PointerProperties();
            prop.id = id;
            prop.toolType = Configurator.getInstance().getToolType();
            coords = new PointerCoords();
            coords.pressure = 1;
            coords.size = 1;
            coords.x = point.x;
            coords.y = point.y;
        }

        public void updatePosition(Point point) {
            coords.x = point.x;
            coords.y = point.y;
        }
    }

    /**
     * Runnable wrapper around a {@link GestureController#performGesture} call.
     */
    private class GestureRunnable implements Runnable {
        private PointerGesture[] mGestures;

        public GestureRunnable(PointerGesture[] gestures) {
            mGestures = gestures;
        }

        @Override
        public void run() {
            performGesture(mGestures);
        }
    }

    UiDevice getDevice() {
        return mDevice;
    }
}
