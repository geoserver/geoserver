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
public enum VerticalAlignmentEnum {

    BASELINE("baseline"), TOP("top"), MIDDLE("middle"), BOTTOM("bottom");
    private final String alignment;

    public String getAlignment() {
        return alignment;
    }

    private VerticalAlignmentEnum(String align) {
        this.alignment = align;
    }
    
}
