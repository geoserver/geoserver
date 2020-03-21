/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.symbol;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class SimpleMarkerSymbol extends MarkerSymbol {

    private SimpleMarkerSymbolEnum style;

    private int[] color;

    private double size;

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

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public Outline getOutline() {
        return outline;
    }

    public void setOutline(Outline outline) {
        this.outline = outline;
    }

    public SimpleMarkerSymbol(SimpleMarkerSymbolEnum style, int[] color, double size, double angle,
            double xoffset, double yoffset, Outline outline) {
        super("esriSMS", angle, xoffset, yoffset);
        this.style = style;
        this.color = color;
        this.size = size;
        this.outline = outline;
    }
}
