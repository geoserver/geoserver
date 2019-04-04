/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.util.List;

public class WfsStoredQueryConstraintsType extends LimitedAreaRequestConstraints {

    List<String> storedQueryIds;

    public WfsStoredQueryConstraintsType() {
        super();
    }

    public List<String> getStoredQueryIds() {
        return storedQueryIds;
    }

    public void setStoredQueryIds(List<String> storedQueryIds) {
        this.storedQueryIds = storedQueryIds;
    }
}
