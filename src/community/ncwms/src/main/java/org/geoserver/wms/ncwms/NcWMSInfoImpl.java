/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

public class NcWMSInfoImpl implements NcWmsInfo {

    private int timeSeriesPoolSize;

    @Override
    public int getTimeSeriesPoolSize() {
        return timeSeriesPoolSize;
    }

    @Override
    public void setTimeSeriesPoolSize(int timeSeriesPoolSize) {
        this.timeSeriesPoolSize = timeSeriesPoolSize;
    }
}
