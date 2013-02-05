/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.symbol;

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

    private SimpleMarkerSymbolEnum(String style) {
        this.style = style;
    }
}
