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
public enum FontDecorationEnum {

    LINE_THROUGH("line-through"), UNDERLINE("underline"), NONE("none");
    private final String style;

    public String getStyle() {
        return style;
    }

    FontDecorationEnum(String style) {
        this.style = style;
    }
}
