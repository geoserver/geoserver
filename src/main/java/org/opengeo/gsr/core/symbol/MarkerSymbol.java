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
public class MarkerSymbol extends Symbol {

    private double angle;

    private int xoffset;

    private int yoffset;

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
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

    public MarkerSymbol(String type, double angle, int xoffset,
            int yoffset) {
        super(type);
        this.angle = angle;
        this.xoffset = xoffset;
        this.yoffset = yoffset;
    }

}
