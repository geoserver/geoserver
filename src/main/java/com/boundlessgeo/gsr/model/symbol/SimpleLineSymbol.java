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
public class SimpleLineSymbol extends Symbol {

    private SimpleLineSymbolEnum style;

    private int[] color;

    private double width;

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

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public SimpleLineSymbol(SimpleLineSymbolEnum style, int[] color, double width) {
        super("esriSLS");
        this.style = style;
        this.color = color;
        this.width = width;
    }

}
