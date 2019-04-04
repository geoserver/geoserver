/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import java.awt.Color;
import org.geoserver.ows.KvpParser;
import org.geoserver.platform.ServiceException;

/**
 * Parses kvp of hte form &lt;key>=&lt;hex color value>.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class ColorKvpParser extends KvpParser {
    public ColorKvpParser(String key) {
        super(key, Color.class);
    }

    public Object parse(String value) throws Exception {
        try {
            return Color.decode(value);
        } catch (NumberFormatException nfe) {
            throw new ServiceException(
                    "BGCOLOR " + value + " incorrectly specified (0xRRGGBB format expected)");
        }
    }
}
