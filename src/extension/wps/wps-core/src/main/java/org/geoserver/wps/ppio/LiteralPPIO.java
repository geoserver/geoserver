/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import org.geotools.util.Converters;

/**
 * Process parameter input / output for literals in string form.
 *
 * <p>This class can handle arbitary literal data and does so by delegating to the geotools
 * converter api, {@link Converters}. However this class may be subclassed as needed.
 *
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, OpenGEO
 */
public class LiteralPPIO extends ProcessParameterIO {

    public LiteralPPIO(Class type) {
        super(type, type);
    }

    /** Decodes the parameter (as a string) to its internal object implementation. */
    public Object decode(String value) throws Exception {
        return Converters.convert(value, getType());
    }

    /** Encodes the internal object representation of a parameter as a string. */
    public String encode(Object value) throws Exception {
        return Converters.convert(value, String.class);
    }
}
