/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.net.URI;
import org.geoserver.ows.KvpParser;

/**
 * Kvp Parser which parses srsName strings like "epsg:4326" into a URI.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class SrsNameKvpParser extends KvpParser {

    public SrsNameKvpParser() {
        super("srsName", URI.class);
    }

    public Object parse(String token) throws Exception {
        return new URI(token);
    }
}
