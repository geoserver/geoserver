/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.util.Locale;
import org.apache.wicket.util.convert.IConverter;
import org.geotools.util.Converter;

public class GeoToolsConverterAdapter implements IConverter<Object> {

    private static final long serialVersionUID = -3177870394414885877L;
    Converter myConverter;
    Class<?> myTarget;

    public GeoToolsConverterAdapter(Converter c, Class<?> target) {
        myConverter = c;
        myTarget = target;
    }

    public Object convertToObject(String str, Locale locale) {
        try {
            return myConverter.convert(str, myTarget);
        } catch (Exception e) {
            return null;
        }
    }

    public String convertToString(Object o, Locale locale) {
        try {
            return myConverter.convert(o, String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
