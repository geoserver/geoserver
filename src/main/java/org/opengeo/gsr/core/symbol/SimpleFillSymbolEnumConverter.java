/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.symbol;

import org.opengeo.gsr.core.format.EnumTypeConverter;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class SimpleFillSymbolEnumConverter extends EnumTypeConverter {

    public SimpleFillSymbolEnumConverter() {
        super(SimpleFillSymbolEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof SimpleFillSymbolEnum) {
            SimpleFillSymbolEnum fieldType = (SimpleFillSymbolEnum) obj;
            switch (fieldType) {
            case BACKWARD_DIAGONAL:
                str = "esriSFSBackwardDiagonal";
                break;
            case CROSS:
                str = "esriSFSCross";
                break;
            case DIAGONAL_CROSS:
                str = "esriSFSDiagonalCross";
                break;
            case FORWARD_DIAGONAL:
                str = "esriSFSForwardDiagonal";
                break;
            case HOLLOW:
                str = "esriSFSHollow";
                break;
            case NULL:
                str = "esriSFSNull";
                break;
            case SOLID:
                str = "esriSFSSolid";
                break;
            case VERTICAL:
                str = "esresriiSFSVertical";
                break;
            }
        }
        return str;
    }
}
