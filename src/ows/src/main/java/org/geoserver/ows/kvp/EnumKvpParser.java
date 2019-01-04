/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import org.geoserver.ows.KvpParser;
import org.springframework.util.Assert;

/** Parses double kvp's of the form 'key=&lt;enum value&gt;'. */
public class EnumKvpParser extends KvpParser {
    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public EnumKvpParser(String key, Class<?> enumClass) {
        super(key, enumClass);
        Assert.isTrue(enumClass.isEnum(), enumClass.getName() + " is not an enum class");
    }

    public Object parse(final String value) throws Exception {
        final Class<?> enumClass = getBinding();
        Object[] enumConstants = enumClass.getEnumConstants();
        for (Object enumValue : enumConstants) {
            if (enumValue.toString().equalsIgnoreCase(value)) {
                return enumValue;
            }
        }
        return null;
    }
}
