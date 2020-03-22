/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.label;

import com.fasterxml.jackson.annotation.JsonValue;

/** @author Juan Marin, OpenGeo */
public enum PolygonLabelPlacementEnum {
    ALWAYS_HORIZONTAL("always-horizontal");
    private final String placement;

    @JsonValue
    public String getPlacement() {
        return placement;
    }

    PolygonLabelPlacementEnum(String placement) {
        this.placement = placement;
    }
}
