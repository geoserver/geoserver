/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.format;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

/**
 * @author Juan Marin, OpenGeo
 */
@SuppressWarnings("rawtypes")
public class EnumTypeConverter extends AbstractSingleValueConverter {

    private final Class enumType;

    public EnumTypeConverter(Class type) {
        this.enumType = type;
    }

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(enumType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object fromString(String value) {
        return Enum.valueOf(enumType, value);
    }

}
