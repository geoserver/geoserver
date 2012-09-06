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
public class SimpleMarkerSymbolEnumConverter extends EnumTypeConverter {

    public SimpleMarkerSymbolEnumConverter() {
        super(SimpleMarkerSymbolEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof SimpleMarkerSymbolEnum) {
            SimpleMarkerSymbolEnum fieldType = (SimpleMarkerSymbolEnum) obj;
            switch (fieldType) {
            case CIRCLE:
                str = "SMSCircle";
                break;
            case CROSS:
                str = "SMSCross";
                break;
            case DIAMOND:
                str = "SMSDiamond";
                break;
            case SQUARE:
                str = "SMSSquare";
                break;
            case X:
                str = "SMSX";
                break;
            }
        }
        return str;
    }
}
