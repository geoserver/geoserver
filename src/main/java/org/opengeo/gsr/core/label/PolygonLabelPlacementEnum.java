/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
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
