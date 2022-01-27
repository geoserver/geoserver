/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.wms.WMS;

/** ncWMS specific parser enforcing its locally configured max times for GetTimeSeries */
public class TimeKvpParser extends org.geoserver.ows.kvp.TimeKvpParser {

    private final WMS wms;

    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public TimeKvpParser(String key, WMS wms) {
        super(key);
        this.wms = wms;
        setService("WMS");
        setRequest("GetTimeSeries");
    }

    @Override
    protected TimeParser getTimeParser() {
        return new TimeParser(NcWmsService.getMaxDimensions(wms));
    }
}
