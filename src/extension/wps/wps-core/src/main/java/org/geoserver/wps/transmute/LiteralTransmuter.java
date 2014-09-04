/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.transmute;

/**
 * LiteralTransmuter interface
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public interface LiteralTransmuter extends Transmuter {
    /**
     * Returns string identifier for encoded value type
     *
     * @return
     */
    String getEncodedType();

    /**
     * Decode string value into Java type
     *
     * @param str
     * @return
     */
    Object decode(String str);

    /**
     * Encode from Java type to String
     *
     * @param obj
     * @return
     */
    String encode(Object obj);
}
