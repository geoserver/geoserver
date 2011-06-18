/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.util.Locale;

import org.apache.wicket.util.convert.IConverter;
import org.geotools.util.Converter;

public class GeoToolsConverterAdapter implements IConverter {
    Converter myConverter;
    Class<?> myTarget;

    public GeoToolsConverterAdapter(Converter c, Class<?> target){
        myConverter = c;
        myTarget = target;
    }

    public Object convertToObject(String str, Locale locale){
        try{
            return myConverter.convert(str, myTarget);
        } catch (Exception e){
            return null;
        }

    }

    public String convertToString(Object o, Locale locale){
        try{
            return myConverter.convert(o, String.class);
        } catch (Exception e){
            return null;
        }
    }
}
