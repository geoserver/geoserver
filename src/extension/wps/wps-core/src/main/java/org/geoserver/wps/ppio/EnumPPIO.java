/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.lang.reflect.Method;

/** A PPIO for handling input literals that are backed by an enum parameter. */
public class EnumPPIO extends LiteralPPIO {

    public EnumPPIO(Class type) {
        super(type);
    }

    @Override
    public Object decode(String value) throws Exception {
        if (value == null) {
            throw new IllegalArgumentException("Unable to look up enum value from null");
        }

        Method valueOf = getType().getMethod("valueOf", String.class);
        try {
            return valueOf.invoke(null, value);
        } catch (IllegalAccessException e) {
            // means we can't have access to the enum, fall back to literal
            return new LiteralPPIO(getType()).decode(value);
        } catch (Exception e) {
            // try upper case
            try {
                return valueOf.invoke(null, value.toUpperCase());
            } catch (Exception e1) {
                // try lower case
                try {
                    return valueOf.invoke(null, value.toLowerCase());
                } catch (Exception e2) {
                    // give up and throw back first exception
                    throw e;
                }
            }
        }
    }

    @Override
    public String encode(Object value) throws Exception {
        return ((Enum) value).name();
    }
}
