/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.impl.ServiceInfoImpl;

/**
 * @see GSSInfo
 * @author aaime
 * 
 */
public class GSSInfoImpl extends ServiceInfoImpl implements GSSInfo {

    DataStoreInfo versioningDataStore;

    GSSMode mode;

    public GSSMode getMode() {
        return mode;
    }

    public void setMode(GSSMode mode) {
        this.mode = mode;
    }

    public GSSInfoImpl() {
        setId("gss");
        setName("GSS");
    }

    public DataStoreInfo getVersioningDataStore() {
        return versioningDataStore;
    }

    public void setVersioningDataStore(DataStoreInfo versioningDataStore) {
        this.versioningDataStore = versioningDataStore;
    }

}
