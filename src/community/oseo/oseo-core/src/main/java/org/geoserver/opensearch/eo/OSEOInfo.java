/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.List;
import org.geoserver.config.ServiceInfo;
import org.geotools.util.Version;

/**
 * OpenSearch for EO service descriptor
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface OSEOInfo extends ServiceInfo {

    int DEFAULT_MAXIMUM_RECORDS = 100;
    int DEFAULT_RECORDS_PER_PAGE = 10;

    /** Version 1.0.0 */
    Version VERSION_1_0_0 = new Version("1.0.0");

    /** Returns the identifier of the OpenSearchAccess */
    String getOpenSearchAccessStoreId();

    void setOpenSearchAccessStoreId(String openSearchAccessStoreId);

    /** Returns the maximum amount of records returned in a search */
    int getMaximumRecordsPerPage();

    /** Sets the maximum amount of records returned in a search */
    void setMaximumRecordsPerPage(int maximumRecords);

    /** Returns the default records per page when no "count" parameter is provided */
    int getRecordsPerPage();

    /** Sets the records per page, when no record is provided */
    void setRecordsPerPage(int recordsPerPage);

    /**
     * Live list of configured product classes. If none is configured, then a clone of {@link
     * ProductClass#DEFAULT_PRODUCT_CLASSES} is returned instead
     */
    List<ProductClass> getProductClasses();
}
