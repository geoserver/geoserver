/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

/** Access limits to a style. */
@SuppressWarnings("serial")
public class StyleAccessLimits extends AccessLimits {

    public StyleAccessLimits(CatalogMode mode) {
        super(mode);
    }
}
