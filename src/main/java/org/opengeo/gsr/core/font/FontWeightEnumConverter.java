/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.font;

import org.opengeo.gsr.core.format.EnumTypeConverter;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class FontWeightEnumConverter extends EnumTypeConverter {

    public FontWeightEnumConverter() {
        super(FontWeightEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof FontWeightEnum) {
            FontWeightEnum fontWeight = (FontWeightEnum) obj;
            switch (fontWeight) {
            case BOLD:
                str = "bold";
                break;
            case BOLDER:
                str = "bolder";
                break;
            case LIGHTER:
                str = "lighter";
                break;
            case NORMAL:
                str = "normal";
                break;
            }
        }
        return str;
    }
}
