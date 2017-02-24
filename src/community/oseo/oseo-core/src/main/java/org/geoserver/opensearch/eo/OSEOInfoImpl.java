/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.geoserver.config.impl.ServiceInfoImpl;

public class OSEOInfoImpl extends ServiceInfoImpl implements OSEOInfo {

    private static final long serialVersionUID = -6834845955630638054L;

    String openSearchAccessStoreId;

    int maximumRecords = OSEOInfo.DEFAULT_MAXIMUM_RECORDS;

    public int getMaximumRecords() {
        return maximumRecords;
    }

    public void setMaximumRecords(int maximumRecords) {
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
