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
public class FontStyleEnumConverter extends EnumTypeConverter {

    public FontStyleEnumConverter() {
        super(FontStyleEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof FontStyleEnum) {
            FontStyleEnum fontStyle = (FontStyleEnum) obj;
            switch (fontStyle) {
            case ITALIC:
                str = "italic";
                break;
            case NORMAL:
                str = "normal";
                break;
            case OBLIQUE:
                str = "oblique";
                break;
            }
        }
        return str;
    }
}
