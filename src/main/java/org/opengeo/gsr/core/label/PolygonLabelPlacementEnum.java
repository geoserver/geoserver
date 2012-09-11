/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.label;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public enum PolygonLabelPlacementEnum {

    ALWAYS_HORIZONTAL("always-horizontal");
    private final String placement;

    public String getPlacement() {
        return placement;
    }

    private PolygonLabelPlacementEnum(String placement) {
        this.placement = placement;
    }
}
