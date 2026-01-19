/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v2_0;

import org.geotools.xsd.XSDParserDelegate;

/**
 * Parser delegate for the WFS 2.0 schema.
 *
 * @author Roar Brænden
 */
public class WFSParserDelegate extends XSDParserDelegate {

    public WFSParserDelegate() {
        super(new WFSConfiguration());
    }
}
