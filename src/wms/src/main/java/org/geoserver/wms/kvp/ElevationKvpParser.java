/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import org.geoserver.ows.kvp.ElevationParser;
import org.geoserver.wms.WMS;

/**
 * A {@link ElevationKvpParser} picking the max number of values to be parsed from the WMS
 * configuration
 */
public class ElevationKvpParser extends org.geoserver.ows.kvp.ElevationKvpParser {

    private final WMS wms;

    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public ElevationKvpParser(String key, WMS wms) {
        super(key);
        this.wms = wms;
    }

    protected ElevationParser getElevationParser() {
        int maxRequestedDimensionValues = wms.getMaxRequestedDimensionValues();
        return new ElevationParser(maxRequestedDimensionValues);
    }
}
