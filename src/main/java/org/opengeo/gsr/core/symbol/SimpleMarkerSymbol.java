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
public class SimpleMarkerSymbol extends MarkerSymbol {

    private SimpleMarkerSymbolEnum style;

    private int[] color;

    private int size;

    private int xoffset;

    private int yoffset;

    private Outline outline;

    public SimpleMarkerSymbolEnum getStyle() {
        return style;
    }

    public void setStyle(SimpleMarkerSymbolEnum style) {
        this.style = style;
    }

    public int[] getColor() {
        return color;
    }

    public void setColor(int[] color) {
        this.color = color;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getXoffset() {
        return xoffset;
    }

    public void setXoffset(int xoffset) {
        this.xoffset = xoffset;
    }

    public int getYoffset() {
        return yoffset;
    }

    public void setYoffset(int yoffset) {
        this.yoffset = yoffset;
    }

    public Outline getOutline() {
        return outline;
    }

    public void setOutline(Outline outline) {
        this.outline = outline;
    }

    public SimpleMarkerSymbol(SimpleMarkerSymbolEnum style, int[] color, int size, int angle,
            int xoffset, int yoffset, Outline outline) {
        super("SMS", angle, xoffset, yoffset);
        this.style = style;
        this.color = color;
        this.size = size;
        this.xoffset = xoffset;
        this.yoffset = yoffset;
        this.outline = outline;
    }
}
