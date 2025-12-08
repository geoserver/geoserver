/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml.v1_0_0;

import org.geotools.wcs.WCSConfiguration;
import org.geotools.xsd.XSDParserDelegate;

/**
 * Parser delegate for WCS 1.0.0.
 *
 * @author Chad Phillips
 */
public class WCSParserDelegate extends XSDParserDelegate {
    public WCSParserDelegate() {
        super(new WCSConfiguration());
    }
}
