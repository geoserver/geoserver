/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.Comparator;
import org.geoserver.catalog.StyleInfo;

/**
 * StyleInfo name comparator
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public class StyleInfoNameComparator implements Comparator<StyleInfo> {

    @Override
    public int compare(StyleInfo s1, StyleInfo s2) {
        return s1.getName().compareToIgnoreCase(s2.getName());
    }
}
