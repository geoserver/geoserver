/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.label;

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

    PolygonLabelPlacementEnum(String placement) {
        this.placement = placement;
    }
}
