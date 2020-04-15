/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.symbol;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.geoserver.gsr.model.font.Font;

/** @author Juan Marin, OpenGeo */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextSymbol extends MarkerSymbol {

    private int[] color;

    private int[] backgroundColor;

    private int[] borderLineColor;

    private Integer haloSize;

    private int[] haloColor;

    private VerticalAlignmentEnum verticalAlignment;

    private HorizontalAlignmentEnum horizontalAlignment;

    private boolean rightToLeft;

    private Font font;

    public int[] getColor() {
        return color;
    }

    public void setColor(int[] color) {
        this.color = color;
    }

    public int[] getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int[] getBorderLineColor() {
        return borderLineColor;
    }

    public void setBorderLineColor(int[] borderLineColor) {
        this.borderLineColor = borderLineColor;
    }

    public VerticalAlignmentEnum getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(VerticalAlignmentEnum verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public HorizontalAlignmentEnum getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(HorizontalAlignmentEnum horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    public boolean isRightToLeft() {
        return rightToLeft;
    }

    public void setRightToLeft(boolean rightToLeft) {
        this.rightToLeft = rightToLeft;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Integer getHaloSize() {
        return haloSize;
    }

    public void setHaloSize(Integer haloSize) {
        this.haloSize = haloSize;
    }

    public int[] getHaloColor() {
        return haloColor;
    }

    public void setHaloColor(int[] haloColor) {
        this.haloColor = haloColor;
    }

    public TextSymbol(
            double angle,
            int xoffset,
            int yoffset,
            int[] color,
            int[] backgroundColor,
            int[] borderLineColor,
            VerticalAlignmentEnum verticalAlignment,
            HorizontalAlignmentEnum horizontalAlignment,
            boolean rightToLeft,
            Font font) {
        super("esriTS", angle, xoffset, yoffset);
        this.color = color;
        this.backgroundColor = backgroundColor;
        this.borderLineColor = borderLineColor;
        this.verticalAlignment = verticalAlignment;
        this.horizontalAlignment = horizontalAlignment;
        this.rightToLeft = rightToLeft;
        this.font = font;
    }
}
