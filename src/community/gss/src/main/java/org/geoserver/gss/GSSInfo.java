/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.ServiceInfo;

/**
 * The configuration of GSS
 */
public interface GSSInfo extends ServiceInfo {

    public enum GSSMode {
        /**
         * Acting as central, the services won't be exposed but a periodic synch thread will be
         * instantiated
         */
        Central,

        /**
         * Acting as a remote unit, the services will be exposed for Central to call and perform the
         * synchronizations
         */
        Unit
    };

    /**
     * The versioning datastore the gss service will use to keep its metadata and the layers to be
     * synchronized
     * 
     * @return
     */
    DataStoreInfo getVersioningDataStore();

    /**
     * Sets the versioning datastore to be used
     * 
     * @param versioningDataStore
     */
    void setVersioningDataStore(DataStoreInfo versioningDataStore);

    /**
     * Returns the mode in which the gss is operating
     */
    public GSSMode getMode();

    /**
     * Sets the current operation mode
     * @param mode
     */
    public void setMode(GSSMode mode);
}
