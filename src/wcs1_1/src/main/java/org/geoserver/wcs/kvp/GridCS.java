/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

/**
 * The only GridCS the WCS 1.1 specification talks about...
 * @author Andrea Aime
 *
 */
public enum GridCS {
    GCSGrid2dSquare("urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS");
    
    private String xmlConstant;

    GridCS(String xmlConstant) {
        this.xmlConstant = xmlConstant;
    }

    public String getXmlConstant() {
        return xmlConstant;
    }
}
