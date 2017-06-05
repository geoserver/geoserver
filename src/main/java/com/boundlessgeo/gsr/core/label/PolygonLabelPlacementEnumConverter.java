/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.label;

import com.boundlessgeo.gsr.core.format.EnumTypeConverter;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class PolygonLabelPlacementEnumConverter extends EnumTypeConverter {

    public PolygonLabelPlacementEnumConverter() {
        super(PolygonLabelPlacementEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof PolygonLabelPlacementEnum) {
            PolygonLabelPlacementEnum labelPlacement = (PolygonLabelPlacementEnum) obj;
            switch (labelPlacement) {
            case ALWAYS_HORIZONTAL:
                str="ServerPolygonPlacementAlwaysHorizontal";
                break;
            }
        }
        return str;
    }
    
}
