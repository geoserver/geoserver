/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.feature;

import org.opengeo.gsr.core.format.EnumTypeConverter;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class FieldTypeConverter extends EnumTypeConverter {

    public FieldTypeConverter() {
        super(FieldTypeEnum.class);

    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof FieldTypeEnum) {
            FieldTypeEnum fieldType = (FieldTypeEnum) obj;
            switch (fieldType) {
            case SHORT_INTEGER:
                str = "FieldTypeSmallInteger";
                break;
            case INTEGER:
                str = "FieldTypeInteger";
                break;
            case SINGLE:
                str = "FieldTypeSingle";
                break;
            case DOUBLE:
                str = "FieldTypeDouble";
                break;
            case STRING:
                str = "FieldTypeString";
                break;
            case DATE:
                str = "FieldTypeDate";
                break;
            case GEOMETRY:
                str = "FieldTypeGeometry";
                break;
            case GUID:
                str = "FieldTypeGUID";
                break;
            case OID:
                str = "FieldTypeOID";
                break;
            }
        }
        return str;
    }

}
