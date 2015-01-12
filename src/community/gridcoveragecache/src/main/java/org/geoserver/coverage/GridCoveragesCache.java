/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import java.io.File;

import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.storage.StorageBroker;

/**
 * A simple classing storing some main Coverage caching configuration properties such as 
 * the {@link StorageBroker} and the {@link GridSetBroker}
 */
public class GridCoveragesCache {

    static final File tempDir;
    static {

        // TODO: Customize this location through Spring
        tempDir = new File(System.getProperty("java.io.tmpdir"));
    }

    public static File getTempdir() {
        return tempDir;
    }

    private GridCoveragesCache() {
    }

    private StorageBroker storageBroker;

    private GridSetBroker gridSetBroker;

    public GridSetBroker getGridSetBroker() {
        return gridSetBroker;
    }

    public void setGridSetBroker(GridSetBroker gridSetBroker) {
        this.gridSetBroker = gridSetBroker;
    }

    public StorageBroker getStorageBroker() {
        return storageBroker;
    }

    public void setStorageBroker(StorageBroker storageBroker) {
        this.storageBroker = storageBroker;
    }

    private GridCoveragesCache(StorageBroker storageBroker, GridSetBroker gridSetBroker) {
        this.storageBroker = storageBroker;
        this.gridSetBroker = gridSetBroker;
    }
}
