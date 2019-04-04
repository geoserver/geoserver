/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.net.URI;
import org.geoserver.ows.FlatKvpParser;

/**
 * Kvp parser that parses to URI.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class URIKvpParser extends FlatKvpParser {

    public URIKvpParser(String key) {
        super(key, URI.class);
    }

    @Override
    protected Object parseToken(String token) throws Exception {
        return new URI(token);
    }
}
