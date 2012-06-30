package org.geoserver.catalog;

import org.geotools.util.Version;

/**
 * Base class for SLD style handlers.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class SLDHandler extends XMLStyleHandler {

    public static final String FORMAT = "sld";

    protected SLDHandler(Version version) {
        super(FORMAT, version);
    }
}
