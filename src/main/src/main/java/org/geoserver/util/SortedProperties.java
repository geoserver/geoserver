/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.util.TreeMap;

/**
 * A Properties implementation that keeps keys sorted in-memory (alphabetical order). Storing will naturally write
 * alphabetically ordered keys.
 */
public class SortedProperties extends AbstractSortedProperties {

    private static final long serialVersionUID = 1L;

    public SortedProperties() {
        super(new TreeMap<>());
    }

    public SortedProperties(java.util.Properties defaults) {
        super(new TreeMap<>(), defaults);
    }
}
