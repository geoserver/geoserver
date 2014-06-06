/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.label;

import org.opengeo.gsr.core.format.EnumTypeConverter;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class PointLabelPlacementEnumConverter extends EnumTypeConverter {

    public PointLabelPlacementEnumConverter() {
        super(PointLabelPlacementEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof PointLabelPlacementEnum) {
            PointLabelPlacementEnum labelPlacement = (PointLabelPlacementEnum) obj;
            switch (labelPlacement) {
            case ABOVE_CENTER:
                str = "ServerPointLabelPlacementAboveCenter";
                break;
            case ABOVE_LEFT:
                str = "ServerPointLabelPlacementAboveLeft";
                break;
            case ABOVE_RIGHT:
                str = "ServerPointLabelPlacementAboveRight";
                break;
            case BELOW_CENTER:
                str = "ServerPointLabelPlacementBelowCenter";
                break;
            case BELOW_LEFT:
                str = "ServerPointLabelPlacementBelowLeft";
                break;
            case BELOW_RIGHT:
                str = "ServerPointLabelPlacementBelowRight";
                break;
            case CENTER_CENTER:
                str = "ServerPointLabelPlacementCenterCenter";
                break;
            case CENTER_LEFT:
                str = "ServerPointLabelPlacementCenterLeft";
                break;
            case CENTER_RIGHT:
                str = "ServerPointLabelPlacementCenterRight";
                break;
            }
        }
        return str;
    }

}
