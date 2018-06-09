/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;

/**
 * Simple parsers used to test parsing of layers format_options that can be transformed to a List by
 * the WMS GetStyles layers parser or remain a String, if the parser is not in context (see
 * GEOS-6402).
 *
 * @author mbarto
 */
public class LayersKvpParser extends KvpParser {

    public static boolean parseAsList = false;

    public LayersKvpParser(String key, Class binding) {
        super(key, binding);
    }

    @Override
    public Object parse(String value) throws Exception {
        if (parseAsList) {
            return KvpUtils.readFlat(value, ",");
        } else {
            return value;
        }
    }
}
