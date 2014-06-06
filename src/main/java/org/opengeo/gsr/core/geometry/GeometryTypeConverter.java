/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.geometry;

import org.opengeo.gsr.core.format.EnumTypeConverter;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class GeometryTypeConverter extends EnumTypeConverter {

    public GeometryTypeConverter() {
        super(GeometryTypeEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof GeometryTypeEnum) {
            GeometryTypeEnum fieldType = (GeometryTypeEnum) obj;
            switch (fieldType) {
            case POINT:
                str = "GeometryPoint";
                break;
            case ENVELOPE:
                str = "GeometryEnvelope";
                break;
            case MULTIPOINT:
                str = "GeometryMultiPoint";
                break;
            case POLYGON:
                str = "GeometryPolygon";
                break;
            case POLYLINE:
                str = "GeometryPolyline";
                break;
            }
        }
        return str;
    }
}
