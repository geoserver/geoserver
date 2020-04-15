/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.symbol;

import com.fasterxml.jackson.annotation.JsonValue;

/** @author Juan Marin, OpenGeo */
public enum VerticalAlignmentEnum {
    BASELINE("baseline"),
    TOP("top"),
    MIDDLE("middle"),
    BOTTOM("bottom");
    private final String alignment;

    @JsonValue
    public String getAlignment() {
        return alignment;
    }

    VerticalAlignmentEnum(String align) {
        this.alignment = align;
    }
}
