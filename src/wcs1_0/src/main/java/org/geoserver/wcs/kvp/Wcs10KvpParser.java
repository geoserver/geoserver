/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import org.geoserver.ows.KvpParser;
import org.geotools.util.Version;

/**
 * Kvp parser specific to WCS 1.0.0
 *
 * <p>This class should be extended by kvp parsers which should only engage on a wcs 1.0.0 request.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author Alessio Fabiani, GeoSolutions
 */
public abstract class Wcs10KvpParser extends KvpParser {

    /** Constructor for use with all wcs 1.0.0 requests. */
    public Wcs10KvpParser(String key, Class<?> binding) {
        this(key, binding, null);
    }

    /** Constrcutor for use with a specific wcs 1.0.0 request. */
    public Wcs10KvpParser(String key, Class<?> binding, String request) {
        super(key, binding);
        setService("wcs");
        setVersion(new Version("1.0.0"));
        setRequest(request);
    }
}
