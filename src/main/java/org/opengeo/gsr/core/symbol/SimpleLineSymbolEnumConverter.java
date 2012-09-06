/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
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
public class SimpleLineSymbolEnumConverter extends EnumTypeConverter {

    public SimpleLineSymbolEnumConverter() {
        super(SimpleLineSymbolEnum.class);
    }

    @Override
    public String toString(Object obj) {
        String str = "";
        if (obj instanceof SimpleLineSymbolEnum) {
            SimpleLineSymbolEnum fieldType = (SimpleLineSymbolEnum) obj;
            switch (fieldType) {
            case DASH:
                str = "SLSDash";
                break;
            case DASH_DOT:
                str = "SLSDashDot";
                break;
            case DASH_DOT_DOT:
                str = "SLSDashDotDot";
                break;
            case DOT:
                str = "SLSDot";
                break;
            case INSIDE_FRAME:
                str = "SLSInsideFrame";
                break;
            case NULL:
                str = "SLSNull";
                break;
            case SOLID:
                str = "SLSSolid";
                break;
            }
        }
        return str;
    }
}
