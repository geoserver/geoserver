/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.util.Comparator;

/**
 * Interface implemented by extensions which require control over the order in which they are processed.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface ExtensionPriority {

    /** The numeric value for highest priority. */
    int HIGHEST = 0;

    /** THe numeric value for lowest priority. */
    int LOWEST = 100;

    /** Compares two implementations based on extension priority */
    Comparator<Object> COMPARATOR = (o1, o2) -> {
        int p1 = ExtensionPriority.LOWEST;
        if (o1 instanceof ExtensionPriority priority) {
            p1 = priority.getPriority();
        }
        int p2 = ExtensionPriority.LOWEST;
        if (o2 instanceof ExtensionPriority priority) {
            p2 = priority.getPriority();
        }
        return p1 - p2;
    };

    /**
     * Returns the priority of the extension.
     *
     * <p>This value is an integer between 0 and 100. Lesser values mean higher priority.
     */
    int getPriority();
}
