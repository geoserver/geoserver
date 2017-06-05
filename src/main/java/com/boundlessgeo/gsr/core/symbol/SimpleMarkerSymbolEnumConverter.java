/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.symbol;

import com.boundlessgeo.gsr.core.format.EnumTypeConverter;

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
                str = "esriSMSCircle";
                break;
            case CROSS:
                str = "esriSMSCross";
                break;
            case DIAMOND:
                str = "esriSMSDiamond";
                break;
            case SQUARE:
                str = "esriSMSSquare";
                break;
            case X:
                str = "esriSMSX";
                break;
            }
        }
        return str;
    }
}
