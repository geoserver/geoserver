/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;

/**
 * Converts between CRS string representations and {@link CoordinateReferenceSystem}
 *
 * @author Andrea Aime - OpenGeo
 */
public class CoordinateReferenceSystemPPIO extends LiteralPPIO {

    public CoordinateReferenceSystemPPIO() {
        super(CoordinateReferenceSystem.class);
    }

    /** Decodes the parameter (as a string) to its internal object implementation. */
    @Override
    public Object decode(String value) throws Exception {
        if (value == null) {
            return null;
        }
        return CRS.decode(value);
    }

    /** Encodes the internal object representation of a parameter as a string. */
    @Override
    public String encode(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        return CRS.lookupIdentifier(((CoordinateReferenceSystem) value), true);
    }
}
