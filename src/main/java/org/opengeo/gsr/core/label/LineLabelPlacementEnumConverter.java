/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.label;

import org.opengeo.gsr.core.format.EnumTypeConverter;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class LineLabelPlacementEnumConverter extends EnumTypeConverter {

    public LineLabelPlacementEnumConverter() {
        super(LineLabelPlacementEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof LineLabelPlacementEnum) {
            LineLabelPlacementEnum labelPlacement = (LineLabelPlacementEnum) obj;
            switch (labelPlacement) {
            case ABOVE_AFTER:
                str = "ServerLinePlacementAboveAfter";
                break;
            case ABOVE_ALONG:
                str = "ServerLinePlacementAboveAlong";
                break;
            case ABOVE_BEFORE:
                str = "ServerLinePlacementAboveBefore";
                break;
            case ABOVE_END:
                str = "ServerLinePlacementAboveEnd";
                break;
            case ABOVE_START:
                str = "ServerLinePlacementAboveStart";
                break;
            case BELOW_AFTER:
                str = "ServerLinePlacementBelowAfter";
                break;
            case BELOW_ALONG:
                str = "ServerLinePlacementBelowAlong";
                break;
            case BELOW_BEFORE:
                str = "ServerLinePlacementBelowBefore";
                break;
            case BELOW_END:
                str = "ServerLinePlacementBelowEnd";
                break;
            case BELOW_START:
                str = "ServerLinePlacementBelowStart";
                break;
            case CENTER_AFTER:
                str = "ServerLinePlacementCenterAfter";
                break;
            case CENTER_ALONG:
                str = "ServerLinePlacementCenterAlong";
                break;
            case CENTER_BEFORE:
                str = "ServerLinePlacementCenterBefore";
                break;
            case CENTER_END:
                str = "ServerLinePlacementCenterEnd";
                break;
            case CENTER_START:
                str = "ServerLinePlacementCenterStart";
                break;
            }
        }
        return str;
    }

}
