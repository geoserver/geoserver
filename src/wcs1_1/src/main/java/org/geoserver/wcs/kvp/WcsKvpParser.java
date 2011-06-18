package org.geoserver.wcs.kvp;

import org.geoserver.ows.KvpParser;
import org.geotools.util.Version;

/**
 * Kvp parser specific to WCS 1.1.
 * <p>
 * This class should be extended by kvp parsers which should only 
 * engage on a wcs 1.1 request.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public abstract class WcsKvpParser extends KvpParser {

    /**
     * Constructor for use with all wcs 1.1 requests.
     */
    public WcsKvpParser(String key, Class binding) {
        this( key, binding, null );
    }

    /**
     * Constrcutor for use with a specific wcs 1.1 request.
     */
    public WcsKvpParser(String key, Class binding, String request ) {
        super(key, binding);
        setService( "wcs" );
        setVersion( new Version( "1.1.1" ) );
        setRequest( request );
    }

}
