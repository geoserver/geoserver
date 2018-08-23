/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.util;

import java.io.Serializable;

public class ValueHolder<T extends Serializable> implements Serializable {

    private T value;

    public ValueHolder() {}

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
