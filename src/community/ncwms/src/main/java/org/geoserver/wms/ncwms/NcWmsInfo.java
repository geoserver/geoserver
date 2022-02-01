/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import java.io.Serializable;

/**
 * NcWMS configuration. Using an interface to allow {@link
 * org.geoserver.catalog.impl.ModificationProxy} to protect the original object before save occurs.
 */
public interface NcWmsInfo extends Serializable {

    /** Returns the number of threads used for parallel computation of GetTimeSeries */
    int getTimeSeriesPoolSize();

    /**
     * Sets the number of threads used for parallel computation of GetTimeSeries
     *
     * @param size A positive number. Zero or negative will be interpreted as picking a default
     *     (equal to the number of CPU cores on the machine running GeoServer)
     */
    void setTimeSeriesPoolSize(int size);

    /**
     * Returns the maximum number of times allowed in a GetTimeSeries
     *
     * @return
     */
    int getMaxTimeSeriesValues();

    /**
     * Sets the maximum number of times allowed in a GetTimeSeries. Use zero or negative for no
     * limit.
     *
     * @param maxTimeValues
     */
    void setMaxTimeSeriesValues(int maxTimeValues);
}
