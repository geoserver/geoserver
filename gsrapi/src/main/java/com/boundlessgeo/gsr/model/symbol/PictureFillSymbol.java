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
public class PictureFillSymbol extends PictureMarkerSymbol {

    private SimpleLineSymbol outline;

    private int xscale;

    private int yscale;

    public SimpleLineSymbol getOutline() {
        return outline;
    }

    public void setOutline(SimpleLineSymbol outline) {
        this.outline = outline;
    }

    public int getXscale() {
        return xscale;
    }

    public void setXscale(int xscale) {
        this.xscale = xscale;
    }

    public int getYscale() {
        return yscale;
    }

    public void setYscale(int yscale) {
        this.yscale = yscale;
    }

    public PictureFillSymbol(byte[] rawData, String url, String contentType, int[] color,
            double width, double height, double angle, int xoffset, int yoffset,
            SimpleLineSymbol outline, int xscale, int yscale) {
        super(rawData, url, contentType, color, width, height, angle, xoffset, yoffset);
        this.setType("PFS");
        this.outline = outline;
        this.xscale = xscale;
        this.yscale = yscale;
    }

}
