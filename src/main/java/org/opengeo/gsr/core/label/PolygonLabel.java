/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
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
public class PolygonLabel extends Label {

    private PolygonLabelPlacementEnum placement;

    public PolygonLabelPlacementEnum getPlacement() {
        return placement;
    }

    public void setPlacement(PolygonLabelPlacementEnum placement) {
        this.placement = placement;
    }

    public PolygonLabel(PolygonLabelPlacementEnum placement, String labelExpression,
            boolean useCodedValues, TextSymbol symbol, int minScale, int maxScale) {
        super(labelExpression, useCodedValues, symbol, minScale, maxScale);
        this.placement = placement;
    }
}
