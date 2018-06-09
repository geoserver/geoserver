/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.transmute;

import org.geoserver.wps.WPSException;

/**
 * LiteralTransmuter for double precission floating point values
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class DoubleTransmuter implements LiteralTransmuter {
    /** @see LiteralTransmuter#decode(String) */
    public Double decode(String encoded) {
        Double decoded;

        try {
            decoded = Double.valueOf(encoded);
        } catch (NumberFormatException e) {
            throw new WPSException("InvalidParameterType", "Could not convert paramter to object.");
        }

        return decoded;
    }

    /** @see Transmuter#getType() */
    public Class<?> getType() {
        return Double.class;
    }

    /** @see LiteralTransmuter#encode(Object) */
    public String encode(Object value) {
        return ((Double) value).toString();
    }

    /** @see LiteralTransmuter#getEncodedType() */
    public String getEncodedType() {
        return "xs:double";
    }
}
