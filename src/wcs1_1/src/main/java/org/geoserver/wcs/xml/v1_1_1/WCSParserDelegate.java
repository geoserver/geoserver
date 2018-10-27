/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml.v1_1_1;

import org.geotools.wcs.v1_1.WCSConfiguration;
import org.geotools.xsd.XSDParserDelegate;

public class WCSParserDelegate extends XSDParserDelegate {

    public WCSParserDelegate() {
        super(new WCSConfiguration());
    }
}
