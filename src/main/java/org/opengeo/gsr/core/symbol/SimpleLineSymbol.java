/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.symbol;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class SimpleLineSymbol extends Symbol {

    private SimpleLineSymbolEnum style;

    private int[] color;

    private int width;

    public SimpleLineSymbolEnum getStyle() {
        return style;
    }

    public void setStyle(SimpleLineSymbolEnum style) {
        this.style = style;
    }

    public int[] getColor() {
        return color;
    }

    public void setColor(int[] color) {
        this.color = color;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public SimpleLineSymbol(SimpleLineSymbolEnum style, int[] color, int width) {
        super("SLS");
        this.style = style;
        this.color = color;
        this.width = width;
    }

}
