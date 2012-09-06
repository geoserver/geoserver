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

    SOLID("SFSSolid"), NULL("SFSNull"), HOLLOW("SFSHollow"), VERTICAL("SFSVertical"), FORWARD_DIAGONAL(
            "SFSForwardDiagonal"), BACKWARD_DIAGONAL("SFSBackwardDiagonal"), CROSS("SFSCross"), DIAGONAL_CROSS(
            "SFSDiagonalCross");
    private final String style;

    public String getStyle() {
        return style;
    }

    private SimpleFillSymbolEnum(String style) {
        this.style = style;
    }
}
