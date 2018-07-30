/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.font;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class Font {

    private String family;

    private int size;

    private FontStyleEnum style;

    private FontWeightEnum weight;

    private FontDecorationEnum decoration;

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public FontStyleEnum getStyle() {
        return style;
    }

    public void setStyle(FontStyleEnum style) {
        this.style = style;
    }

    public FontWeightEnum getWeight() {
        return weight;
    }

    public void setWeight(FontWeightEnum weight) {
        this.weight = weight;
    }

    public FontDecorationEnum getDecoration() {
        return decoration;
    }

    public void setDecoration(FontDecorationEnum decoration) {
        this.decoration = decoration;
    }

    public Font(String family, int size, FontStyleEnum style, FontWeightEnum weight,
            FontDecorationEnum decoration) {
        super();
        this.family = family;
        this.size = size;
        this.style = style;
        this.weight = weight;
        this.decoration = decoration;
    }

}
