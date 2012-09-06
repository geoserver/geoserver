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
public enum SimpleLineSymbolEnum {

    SOLID("SLSSolid"), DASH("SLSDash"), DOT("SLSDot"), DASH_DOT("SLSDashDot"), DASH_DOT_DOT(
            "SLSDashDotDot"), NULL("SLSNull"), INSIDE_FRAME("SLSInsideFrame");
    private final String style;

    public String getStyle() {
        return style;
    }

    private SimpleLineSymbolEnum(String style) {
        this.style = style;
    }
}
