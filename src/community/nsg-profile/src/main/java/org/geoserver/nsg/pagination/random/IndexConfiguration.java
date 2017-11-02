/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.nsg.pagination.random;

import org.geoserver.platform.resource.Resource;
import org.geotools.data.DataStore;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class used to store the index result type configuration managed by {@link IndexInitializer}
 *
 * @author sandr
 */
public class IndexConfiguration {

    private DataStore currentDataStore;

    private Resource storageResource;

    private Long timeToLiveInSec = 600L;

    private Map<String, Object> currentDataStoreParams;

    /**
     * Store the DB parameters and the relative {@link DataStore}
     *
     * @param currentDataStoreParams
     * @param currentDataStore
     */
    public void setCurrentDataStore(Map<String, Object> currentDataStoreParams,
                                    DataStore currentDataStore) {
        this.currentDataStoreParams = currentDataStoreParams;
        this.currentDataStore = currentDataStore;
    }

    /**
     * Store the reference to resource used to archive the serialized GetFeatureRequest
     *
     * @param storageResource
     */
    public void setStorageResource(Resource storageResource) {
        this.storageResource = storageResource;
    }

    /**
     * Store the value of time to live of stored GetFeatureRequest
     *
     * @param timeToLive
     * @param timeUnit
     */
    public void setTimeToLive(Long timeToLive, TimeUnit timeUnit) {
        this.timeToLiveInSec = timeUnit.toSeconds(timeToLive);
    }

    public DataStore getCurrentDataStore() {
        return currentDataStore;
    }

    public Map<String, Object> getCurrentDataStoreParams() {
        return currentDataStoreParams;
    }

    public Resource getStorageResource() {
        return storageResource;
    }

    public Long getTimeToLiveInSec() {
        return timeToLiveInSec;
    }

}
