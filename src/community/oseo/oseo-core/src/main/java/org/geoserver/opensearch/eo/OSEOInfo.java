/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.geoserver.config.ServiceInfo;
import org.geotools.util.Version;

public interface OSEOInfo extends ServiceInfo {
    
    public static int DEFAULT_MAXIMUM_RECORDS = 50;

    /**
     * Version 1.0.0
     */
    public static final Version VERSION_1_0_0 = new Version("1.0.0");

    /**
     * Returns the identifier of the OpenSearchAccess
     * 
     * @return
     */
    String getOpenSearchAccessStoreId();

    void setOpenSearchAccessStoreId(String openSearchAccessStoreId);

    /**
     * Returns the maximum amount of records returned in a search
     * 
     * @return
     */
    int getMaximumRecords();

    /**
     * Sets the maximum amount of records returned in a search
     * 
     * @param maximumRecords
     */
    void setMaximumRecords(int maximumRecords);

}
