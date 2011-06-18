/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.transmute;

/**
 * Transmuter for GML2 LinearRing geometries
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class GML2LinearRingTransmuter extends GML2ComplexTransmuter {
    /**
     * @see ComplexTransmuter#getSchema(String)
     */
    public String getSchema(String urlBase) {
        return urlBase + "ows?service=WPS&request=GetSchema&Identifier=gml2linearring.xsd";
    }
}