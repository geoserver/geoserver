/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.label;

import com.boundlessgeo.gsr.model.symbol.TextSymbol;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class LineLabel extends Label {

    private LineLabelPlacementEnum placement;

    public LineLabelPlacementEnum getPlacement() {
        return placement;
    }

    public void setPlacement(LineLabelPlacementEnum placement) {
        this.placement = placement;
    }

    public LineLabel(LineLabelPlacementEnum placement, String labelExpression,
            boolean useCodedValues, TextSymbol symbol, int minScale, int maxScale) {
        super(labelExpression, useCodedValues, symbol, minScale, maxScale);
        this.placement = placement;
    }
}
