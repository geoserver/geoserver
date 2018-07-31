/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.feature;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import com.vividsolutions.jts.geom.Geometry;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public enum FieldTypeEnum {

    SHORT_INTEGER("esriFieldTypeSmallInteger"),
    INTEGER("esriFieldTypeInteger"),
    SINGLE("esriFieldTypeSingle"),
    DOUBLE("esriFieldTypeDouble"),
    STRING("esriFieldTypeString"),
    DATE("esriFieldTypeDate"),
    OID("esriFieldTypeOID"),
    GEOMETRY("esriFieldTypeGeometry"),
    GUID("esriFieldTypeGUID"),
    GLOBAL_ID("esriFieldTypeGlobalID"),
    XML("esriFieldTypeXML");

    private final String fieldType;

    public String getFieldType() {
        return fieldType;
    }

    FieldTypeEnum(String fieldType) {
        this.fieldType = fieldType;
    }

    public static FieldTypeEnum forClass(Class<?> binding) {
        if (String.class.equals(binding)) {
            return STRING;
        } else if (Float.class.equals(binding)) {
            return SINGLE;
        } else if (Double.class.equals(binding) || BigDecimal.class.equals(binding)) {
            return DOUBLE;
        } else if (Boolean.class.equals(binding) || Byte.class.equals(binding) || Short.class.equals(binding)) {
            return SHORT_INTEGER;
        } else if (Integer.class.equals(binding) || Long.class.equals(binding) || BigInteger.class.equals(binding)) {
            return INTEGER;
        } else if (Date.class.isAssignableFrom(binding)) {
            return DATE;
        } else if (Geometry.class.isAssignableFrom(binding)) {
            return GEOMETRY;
        } else {
            throw new RuntimeException("No FieldType equivalent known for " + binding);
        }
    }
}
