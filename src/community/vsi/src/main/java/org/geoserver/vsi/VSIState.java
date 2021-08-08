/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import org.geoserver.catalog.StoreInfo;

/**
 * Helper class for managing state of VSI Virutal File System StoreInfo objects Should only be set
 * by VSIInfoPanel
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public class VSIState {

    private static StoreInfo storeInfo = null;

    /**
     * Getter method for storeInfo
     *
     * @return current StoreInfo object
     */
    public static StoreInfo getStoreInfo() {
        return storeInfo;
    }

    /**
     * Setter method for storeInfo
     *
     * @param store StoreInfo object to replace current state
     */
    public static void setStoreInfo(StoreInfo store) {
        storeInfo = store;
    }
}
