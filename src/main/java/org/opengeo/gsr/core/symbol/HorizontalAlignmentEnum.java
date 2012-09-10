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
public enum HorizontalAlignmentEnum {

    LEFT("left"), RIGHT("right"), CENTER("center"), JUSTIFY("justify");
    private final String alignment;

    public String getAlignment() {
        return alignment;
    }

    private HorizontalAlignmentEnum(String align) {
        this.alignment = align;
    }
}
