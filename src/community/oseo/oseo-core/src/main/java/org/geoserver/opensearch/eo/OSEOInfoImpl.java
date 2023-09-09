/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.config.impl.ServiceInfoImpl;

public class OSEOInfoImpl extends ServiceInfoImpl implements OSEOInfo {

    private static final long serialVersionUID = -6834845955630638054L;

    String openSearchAccessStoreId;

    int maximumRecords = DEFAULT_MAXIMUM_RECORDS;

    int recordsPerPage = DEFAULT_RECORDS_PER_PAGE;

    Integer aggregatesCacheTTL = DEFAULT_AGGR_CACHE_TTL;

    String aggregatesCacheTTLUnit = DEFAULT_AGGR_CACHE_TTL_UNIT;

    List<ProductClass> productClasses = new ArrayList<>(ProductClass.DEFAULT_PRODUCT_CLASSES);

    String attribution;

    List<String> globalQueryables = new ArrayList<>();

    @Override
    public int getRecordsPerPage() {
        return recordsPerPage == 0 ? DEFAULT_RECORDS_PER_PAGE : recordsPerPage;
    }

    @Override
    public void setRecordsPerPage(int defaultRecords) {
        this.recordsPerPage = defaultRecords;
    }

    @Override
    public List<ProductClass> getProductClasses() {
        // XStream deserialization bypasses constructor, mind
        if (productClasses == null) {
            productClasses = new ArrayList<>(ProductClass.DEFAULT_PRODUCT_CLASSES);
        }
        return productClasses;
    }

    @Override
    public int getMaximumRecordsPerPage() {
        return maximumRecords;
    }

    @Override
    public void setMaximumRecordsPerPage(int maximumRecords) {
        this.maximumRecords = maximumRecords;
    }

    @Override
    public String getOpenSearchAccessStoreId() {
        return openSearchAccessStoreId;
    }

    @Override
    public void setOpenSearchAccessStoreId(String openSearchAccessStoreId) {
        this.openSearchAccessStoreId = openSearchAccessStoreId;
    }

    @Override
    public String getAttribution() {
        return attribution;
    }

    @Override
    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    @Override
    public List<String> getGlobalQueryables() {
        return globalQueryables;
    }

    public void setGlobalQueryables(List<String> globalQueryables) {
        this.globalQueryables = globalQueryables;
    }

    @Override
    public Integer getAggregatesCacheTTL() {
        return aggregatesCacheTTL;
    }

    @Override
    public void setAggregatesCacheTTL(Integer aggregatesCacheTTL) {
        this.aggregatesCacheTTL = aggregatesCacheTTL;
    }

    @Override
    public String getAggregatesCacheTTLUnit() {
        return aggregatesCacheTTLUnit;
    }

    @Override
    public void setAggregatesCacheTTLUnit(String aggregatesCacheTTLUnit) {
        this.aggregatesCacheTTLUnit = aggregatesCacheTTLUnit;
    }
}
