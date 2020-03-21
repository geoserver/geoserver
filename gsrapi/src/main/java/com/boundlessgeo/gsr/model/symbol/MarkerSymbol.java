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
public class MarkerSymbol extends Symbol {

    private double angle;

    private double xoffset;

    private double yoffset;

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getXoffset() {
        return xoffset;
    }

    public void setXoffset(double xoffset) {
        this.xoffset = xoffset;
    }

    public double getYoffset() {
        return yoffset;
    }

    public void setYoffset(double yoffset) {
        this.yoffset = yoffset;
    }

    public MarkerSymbol(String type, double angle, double xoffset, double yoffset) {
        super(type);
        this.angle = angle;
        this.xoffset = xoffset;
        this.yoffset = yoffset;
    }

}
