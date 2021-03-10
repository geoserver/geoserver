/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data;

import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

/** A factory finder for JDBCDataStore. */
public class JDBCDataStoreFactoryFinder {

    /**
     * @param type the data store type
     * @return the JDBCDataStoreFactory
     */
    public JDBCDataStoreFactory getFactoryFromType(String type) {
        if (type.toUpperCase().contains(SupportedStoreType.POSTGIS.name()))
            return new PostgisNGDataStoreFactory();
        throw new RuntimeException("Unsupported DataStore type " + type);
    }

    public enum SupportedStoreType {
        POSTGIS
    }
}
