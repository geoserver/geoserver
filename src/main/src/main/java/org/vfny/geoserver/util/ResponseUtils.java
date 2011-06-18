/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import java.io.IOException;
import java.io.Writer;


/**
 * @deprecated use {@link org.geoserver.ows.util.ResponseUtils}.
 *
 */
public final class ResponseUtils {
    /**
     * @deprecated moved to {@link org.geoserver.ows.util.ResponseUtils#encodeXML(String)}.
     */
    public static String encodeXML(String inData) {
        return org.geoserver.ows.util.ResponseUtils.encodeXML(inData);
    }

    /**
     * @deprecated moved to {@link org.geoserver.ows.util.ResponseUtils#writeEscapedString(Writer, String)}
     */
    public static void writeEscapedString(Writer writer, String string)
        throws IOException {
        org.geoserver.ows.util.ResponseUtils.writeEscapedString(writer, string);
    }
}
