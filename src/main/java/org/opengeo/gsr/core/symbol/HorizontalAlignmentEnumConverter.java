/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.symbol;

import org.opengeo.gsr.core.format.EnumTypeConverter;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class HorizontalAlignmentEnumConverter extends EnumTypeConverter {

    public HorizontalAlignmentEnumConverter() {
        super(HorizontalAlignmentEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof HorizontalAlignmentEnum) {
            HorizontalAlignmentEnum horizontalAlignment = (HorizontalAlignmentEnum) obj;
            switch (horizontalAlignment) {
            case LEFT:
                str = "left";
                break;
            case RIGHT:
                str = "right";
                break;
            case CENTER:
                str = "center";
                break;
            case JUSTIFY:
                str = "justify";
                break;
            }
        }
        return str;
    }
}
