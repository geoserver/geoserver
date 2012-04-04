/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.impl;


/**
 * Property stored as a user property.
 * <p>
 * This class sets/unsets values from {@Link GeoServerUser#getProperties()} 
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class UserProperty<T> {

    /** the property key */
    String key;

    protected UserProperty(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String toString(T value) {
        return value != null ? value.toString() : null;
    }

    public abstract T fromString(String value);
}
