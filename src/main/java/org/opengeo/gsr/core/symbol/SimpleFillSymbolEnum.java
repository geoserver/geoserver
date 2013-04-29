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
public enum SimpleFillSymbolEnum {

    SOLID("esriSFSSolid"), //
    NULL("esriSFSNull"), //
    HOLLOW("esriSFSHollow"), //
    VERTICAL("esriSFSVertical"), //
    FORWARD_DIAGONAL("esriSFSForwardDiagonal"), //
    BACKWARD_DIAGONAL("esriSFSBackwardDiagonal"), //
    CROSS("esriSFSCross"), //
    DIAGONAL_CROSS("esriSFSDiagonalCross");

    private final String style;

    public String getStyle() {
        return style;
    }

    private SimpleFillSymbolEnum(String style) {
        this.style = style;
    }
}
