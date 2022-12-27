/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package androidx.test.uiautomator;

import java.util.List;

/** The Searchable interface represents an object that can be searched for matching UI elements. */
interface Searchable {

    /** Returns whether there is a match for the given {@code selector} criteria. */
    public boolean hasObject(BySelector selector);

    /** Returns the first object to match the {@code selector} criteria. */
    public UiObject2 findObject(BySelector selector);

    /** Returns all objects that match the {@code selector} criteria. */
    public List<UiObject2> findObjects(BySelector selector);
}
