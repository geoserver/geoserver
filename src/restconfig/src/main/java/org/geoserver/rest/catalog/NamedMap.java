/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.util.LinkedHashMap;

/** A map that can be given a name */
@SuppressWarnings("serial")
public class NamedMap<K, T> extends LinkedHashMap<K, T> {

    String name;

    public NamedMap(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
