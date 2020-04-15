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
public enum SimpleFillSymbolEnum {
    SOLID("esriSFSSolid"), //
    NULL("esriSFSNull"), //
    HORIZONTAL("esriSFSHorizontal"), //
    VERTICAL("esriSFSVertical"), //
    FORWARD_DIAGONAL("esriSFSForwardDiagonal"), //
    BACKWARD_DIAGONAL("esriSFSBackwardDiagonal"), //
    CROSS("esriSFSCross"), //
    DIAGONAL_CROSS("esriSFSDiagonalCross");

    private final String style;

    @JsonValue
    public String getStyle() {
        return style;
    }

    SimpleFillSymbolEnum(String style) {
        this.style = style;
    }
}
