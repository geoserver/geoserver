/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.renderer;

import org.geoserver.gsr.model.symbol.Symbol;

/** @author Juan Marin, OpenGeo */
public class SimpleRenderer extends Renderer {

    private Symbol symbol;

    private String label;

    private String description;

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SimpleRenderer(Symbol symbol, String label, String description) {
        super("simple");
        this.symbol = symbol;
        this.label = label;
        this.description = description;
    }
}
