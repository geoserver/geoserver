/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.transmute;

import org.geotools.gml2.GMLConfiguration;
import org.locationtech.jts.geom.Geometry;

/**
 * Abstract transmuter for GML2 geometries
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public abstract class GML2ComplexTransmuter extends ComplexTransmuter {
    /** @see ComplexTransmuter#getXMLConfiguration() */
    public Class<?> getXMLConfiguration() {
        return GMLConfiguration.class;
    }

    /** @see ComplexTransmuter#getMimeType() */
    public String getMimeType() {
        return "text/xml; subtype=gml/2.1.2";
    }

    /** @see Transmuter#getType() */
    public Class<?> getType() {
        return Geometry.class;
    }
}
