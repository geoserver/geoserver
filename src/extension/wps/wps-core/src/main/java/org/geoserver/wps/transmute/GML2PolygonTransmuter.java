/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.transmute;

/**
 * ComplexTransmuter for JTS Geometry to/from GML2 Polygons
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class GML2PolygonTransmuter extends GML2ComplexTransmuter {
    /** @see ComplexTransmuter#getSchema(String) */
    public String getSchema(String urlBase) {
        return urlBase + "ows?service=WPS&request=GetSchema&Identifier=gml2polygon.xsd";
    }
}
