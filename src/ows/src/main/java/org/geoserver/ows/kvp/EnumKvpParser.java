/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import org.geoserver.ows.KvpParser;
import org.springframework.util.Assert;

/**
 * Parses double kvp's of the form 'key=<enum value>'.
 */
public class EnumKvpParser extends KvpParser {
    /**
     * Creates the parser specifying the name of the key to latch to.
     * 
     * @param key
     *            The key whose associated value to parse.
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
