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
public class VerticalAlignmentEnumConverter extends EnumTypeConverter {

    public VerticalAlignmentEnumConverter() {
        super(VerticalAlignmentEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof VerticalAlignmentEnum) {
            VerticalAlignmentEnum verticalAlignment = (VerticalAlignmentEnum) obj;
            switch (verticalAlignment) {
            case BASELINE:
                str = "baseline";
                break;
            case TOP:
                str = "top";
                break;
            case MIDDLE:
                str = "middle";
                break;
            case BOTTOM:
                str = "bottom";
                break;
            }
        }
        return str;
    }
}
