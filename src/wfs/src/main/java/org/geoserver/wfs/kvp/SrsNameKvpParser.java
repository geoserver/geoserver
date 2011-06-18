package org.geoserver.wfs.kvp;

import java.net.URI;

import org.geoserver.ows.FlatKvpParser;
import org.geoserver.ows.KvpParser;

/**
 * Kvp Parser which parses srsName strings like "epsg:4326" into a URI.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class SrsNameKvpParser extends KvpParser {

    public SrsNameKvpParser() {
        super("srsName", URI.class);
        
    }
    
    public Object parse(String token) throws Exception {
        return new URI(token);
    }

}
