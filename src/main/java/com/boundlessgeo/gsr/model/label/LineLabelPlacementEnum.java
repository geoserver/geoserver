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
public enum LineLabelPlacementEnum {

    ABOVE_AFTER("above-after"), ABOVE_ALONG("above-along"), ABOVE_BEFORE("above-before"), ABOVE_END(
            "above-end"), ABOVE_START("above-start"), BELOW_AFTER("below-after"), BELOW_ALONG(
            "below-along"), BELOW_BEFORE("below-before"), BELOW_END("below-end"), BELOW_START(
            "below-start"), CENTER_AFTER("center-after"), CENTER_ALONG("center-along"), CENTER_BEFORE(
            "center-before"), CENTER_END("center-end"), CENTER_START("center-start");
    private final String placement;

    public String getPlacement() {
        return placement;
    }

    LineLabelPlacementEnum(String placement) {
        this.placement = placement;
    }
}
