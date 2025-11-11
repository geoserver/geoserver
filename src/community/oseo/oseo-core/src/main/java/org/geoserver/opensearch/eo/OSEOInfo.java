/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.geoserver.config.ServiceInfo;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfo;
import org.geotools.util.Version;

/**
 * OpenSearch for EO service descriptor
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface OSEOInfo extends ServiceInfo {

    int DEFAULT_MAXIMUM_RECORDS = 100;
    int DEFAULT_RECORDS_PER_PAGE = 10;
    int DEFAULT_AGGR_CACHE_TTL = 1; // 1 hour

    String DEFAULT_AGGR_CACHE_TTL_UNIT = TimeUnit.HOURS.name();

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

    /** Returns the TTL of the aggregates cache, in hours */
    Integer getAggregatesCacheTTL();

    /** Sets the TTL of the aggregates cache, in hours */
    void setAggregatesCacheTTL(Integer aggregatesCacheTTL);

    /** Returns the units of the aggregates cache TTL */
    String getAggregatesCacheTTLUnit();

    /** Sets the units of the aggregates cache TTL */
    void setAggregatesCacheTTLUnit(String aggregatesCacheTTLUnit);

    /**
     * Live list of configured product classes. If none is configured, then a clone of
     * {@link ProductClass#DEFAULT_PRODUCT_CLASSES} is returned instead
     */
    List<ProductClass> getProductClasses();

    /** Sets the OpenSearch Attribution, provided in the OSDD document */
    String getAttribution();

    void setAttribution(String attribution);

    List<String> getGlobalQueryables();

    /** True if this feature type info is overriding the counting of numberMatched. */
    boolean isSkipNumberMatched();

    /** Set to true if this feature type info is overriding the default counting of numberMatched. */
    void setSkipNumberMatched(boolean skipNumberMatched);

    /** Returns the list of collection access limits (as a live, editable collection) */
    List<EOCollectionAccessLimitInfo> getCollectionLimits();

    /** Returns the list of product access limits (as a live, editable collection) */
    List<EOProductAccessLimitInfo> getProductLimits();
}
