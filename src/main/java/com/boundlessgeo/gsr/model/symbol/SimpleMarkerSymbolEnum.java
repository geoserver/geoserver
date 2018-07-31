/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.symbol;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public enum SimpleMarkerSymbolEnum {

    CIRCLE("esriSMSCircle"), SQUARE("esriSMSSquare"), CROSS("esriSMSCross"), X("esriSMSX"), DIAMOND("esriSMSDiamond");
    private final String style;

    public String getStyle() {
        return style;
    }

    SimpleMarkerSymbolEnum(String style) {
        this.style = style;
    }
}
