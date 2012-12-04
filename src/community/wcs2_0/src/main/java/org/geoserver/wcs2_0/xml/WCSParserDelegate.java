/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0.xml;

import org.geotools.wcs.v2_0.WCSConfiguration;
import org.geotools.xml.XSDParserDelegate;

/**
 * Allows WCS requests embedded in other documents to be parsed (mostly used by WPS)
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class WCSParserDelegate extends XSDParserDelegate {

    public WCSParserDelegate() {
        super(new WCSConfiguration());
    }

}
