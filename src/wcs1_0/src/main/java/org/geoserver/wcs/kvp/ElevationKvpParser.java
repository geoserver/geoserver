/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.kvp.ElevationParser;
import org.geoserver.wcs.WCSInfo;
import org.geotools.util.Version;

/**
 * A {@link ElevationKvpParser} picking the max number of values to be parsed from the WCS
 * configuration
 */
public class ElevationKvpParser extends org.geoserver.ows.kvp.ElevationKvpParser {

    private final GeoServer geoServer;

    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public ElevationKvpParser(String key, GeoServer geoServer) {
        super(key);
        setService("WCS");
        setVersion(new Version("1.0.0"));
        this.geoServer = geoServer;
    }

    protected ElevationParser getElevationParser() {
        WCSInfo info = geoServer.getService(WCSInfo.class);
        int maxRequestedDimensionValues = info.getMaxRequestedDimensionValues();
        return new ElevationParser(maxRequestedDimensionValues);
    }
}
