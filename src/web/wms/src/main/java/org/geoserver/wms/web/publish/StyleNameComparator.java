/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.util.Comparator;
import org.geoserver.catalog.StyleInfo;

/** Compares two {@link StyleInfo} by name */
class StyleNameComparator implements Comparator<StyleInfo> {

    public int compare(StyleInfo o1, StyleInfo o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
    }
}
