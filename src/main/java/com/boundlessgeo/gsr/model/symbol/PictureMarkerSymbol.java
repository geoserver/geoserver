/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.symbol;

import org.geotools.data.Base64;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class PictureMarkerSymbol extends MarkerSymbol {

    private String url;

    private String imageData;

    private String contentType;

    private int[] color;

    private double width;

    private double height;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
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

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public PictureMarkerSymbol(byte[] rawData, String url, String contentType, int[] color,
            double width, double height, double angle, int xoffset, int yoffset) {
        super("PMS", angle, xoffset, yoffset);
        this.url = url;
        this.contentType = contentType;
        this.color = color;
        this.imageData = Base64.encodeBytes(rawData, Base64.DONT_BREAK_LINES);
        this.width = width;
        this.height = height;
    }

}
