/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.geoserver.config.ServiceInfo;
import org.geotools.util.Version;

/**
 * OpenSearch for EO service descriptor
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface OSEOInfo extends ServiceInfo {
    
    public static int DEFAULT_MAXIMUM_RECORDS = 100;
    public static int DEFAULT_RECORDS_PER_PAGE = 10;

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
    int getMaximumRecordsPerPage();

    /**
     * Sets the maximum amount of records returned in a search
     * 
     * @param maximumRecords
     */
    void setMaximumRecordsPerPage(int maximumRecords);
    
    /**
     * Returns the default records per page when no "count" parameter is provided
     * @return
     */
    public int getRecordsPerPage();
    
    /**
     * Sets the records per page, when no record is provided
     * @param recordsPerPage
     */
    public void setRecordsPerPage(int recordsPerPage);


}
