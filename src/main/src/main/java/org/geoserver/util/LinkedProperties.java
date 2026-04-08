/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.util.LinkedHashMap;
import java.util.Properties;

/**
 * A Properties subclass that maintains insertion order using a {@link java.util.LinkedHashMap}. LinkedProperties is
 * insertion-order in-memory and will write properties in insertion order when stored. Use {@link SortedProperties} when
 * deterministic alphabetical ordering is required (written alphabetically).
 *
 * <p>Examples:
 *
 * <pre>
 * // Insertion-order: LinkedProperties
 * LinkedProperties props = new LinkedProperties();
 * props.setProperty("zebra", "last");
 * props.setProperty("alpha", "first");
 * props.store(out, null);  // Writes: zebra=last, alpha=first (in insertion order)
 *
 * // Alphabetical: SortedProperties
 * SortedProperties sorted = new SortedProperties();
 * sorted.setProperty("zebra", "last");
 * sorted.setProperty("alpha", "first");
 * sorted.store(out, null);  // Writes: alpha=first, zebra=last (alphabetical)
 * </pre>
 */
public class LinkedProperties extends AbstractSortedProperties {

    private static final long serialVersionUID = 1L;

    public LinkedProperties() {
        super(new LinkedHashMap<>());
    }

    public LinkedProperties(Properties defaults) {
        super(new LinkedHashMap<>(), defaults);
    }
}
