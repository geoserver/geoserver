/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geotools.factory.Hints;
import org.geotools.util.Converter;
import org.geotools.util.ConverterFactory;

/**
 * Converts between enumerations and strings
 * 
 * @author Andrea Aime - OpenGeo
 */
public class EnumerationConverterFactory implements ConverterFactory {

    public Converter createConverter(Class<?> source, Class<?> target, Hints hints) {
        if ((String.class.equals(source) && target.isEnum())
                || (source.isEnum() && String.class.equals(source))) {
            return new EnumConverter();
        } else {
            return null;
        }
    }

    private static class EnumConverter implements Converter {

        public <T> T convert(Object source, Class<T> target) throws Exception {
            if (source instanceof String && target.isEnum()) {
                return (T) Enum.valueOf((Class<Enum>) target, (String) source);
            } else if (source.getClass().isEnum() && String.class.equals(target)) {
                return (T) ((Enum) source).name();
            } else {
                return null;
            }
        }

    }
}
