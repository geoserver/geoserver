/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
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
                str = "SFSBackwardDiagonal";
                break;
            case CROSS:
                str = "SFSCross";
                break;
            case DIAGONAL_CROSS:
                str = "SFSDiagonalCross";
                break;
            case FORWARD_DIAGONAL:
                str = "SFSForwardDiagonal";
                break;
            case HOLLOW:
                str = "SFSHollow";
                break;
            case NULL:
                str = "SFSNull";
                break;
            case SOLID:
                str = "SFSSolid";
                break;
            case VERTICAL:
                str = "SFSVertical";
                break;
            }
        }
        return str;
    }
}
