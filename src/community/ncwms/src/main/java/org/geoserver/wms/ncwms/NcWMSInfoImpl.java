/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

public class NcWMSInfoImpl implements NcWmsInfo {

    private int timeSeriesPoolSize;
    private int maxTimeSeriesValues;

    @Override
    public int getTimeSeriesPoolSize() {
        return timeSeriesPoolSize;
    }

    @Override
    public void setTimeSeriesPoolSize(int timeSeriesPoolSize) {
        this.timeSeriesPoolSize = timeSeriesPoolSize;
    }

    @Override
    public int getMaxTimeSeriesValues() {
        return maxTimeSeriesValues;
    }

    @Override
    public void setMaxTimeSeriesValues(int maxTimeSeriesValues) {
        this.maxTimeSeriesValues = maxTimeSeriesValues;
    }
}
