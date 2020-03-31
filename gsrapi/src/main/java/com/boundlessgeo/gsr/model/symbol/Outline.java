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
public class Outline {

    private int[] color;

    private int width;

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

    public Outline(int[] color, int width) {
        super();
        this.color = color;
        this.width = width;
    }
}
