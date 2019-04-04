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

    int maximumRecords = OSEOInfo.DEFAULT_MAXIMUM_RECORDS;

    int recordsPerPage = OSEOInfo.DEFAULT_RECORDS_PER_PAGE;

    List<ProductClass> productClasses = new ArrayList<>(ProductClass.DEFAULT_PRODUCT_CLASSES);

    public int getRecordsPerPage() {
        return recordsPerPage == 0 ? OSEOInfo.DEFAULT_RECORDS_PER_PAGE : recordsPerPage;
    }

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

    public int getMaximumRecordsPerPage() {
        return maximumRecords;
    }

    public void setMaximumRecordsPerPage(int maximumRecords) {
        this.maximumRecords = maximumRecords;
    }

    public String getOpenSearchAccessStoreId() {
        return openSearchAccessStoreId;
    }

    @Override
    public void setOpenSearchAccessStoreId(String openSearchAccessStoreId) {
        this.openSearchAccessStoreId = openSearchAccessStoreId;
    }
}
