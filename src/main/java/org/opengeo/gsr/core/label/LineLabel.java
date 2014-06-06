/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.label;

import org.opengeo.gsr.core.symbol.TextSymbol;

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
