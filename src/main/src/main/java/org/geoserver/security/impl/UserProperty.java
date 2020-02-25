/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

/**
 * Property stored as a user property.
 *
 * <p>This class sets/unsets values from {@Link GeoServerUser#getProperties()}
 *
 * @author Justin Deoliveira, OpenGeo
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
