/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import org.geoserver.feature.retype.RetypingDataStore;
import org.geotools.api.data.DataStore;

/**
 * Handles idiosyncrasies in database table naming, for the moment only lowercase vs uppercase, later we could extend it
 * to handle property names and the like
 */
class LowercasingDataStore extends RetypingDataStore {

    DataStore wrapped;

    public LowercasingDataStore(DataStore wrapped) throws IOException {
        super(wrapped);
        this.wrapped = wrapped;
    }

    @Override
    protected String transformFeatureTypeName(String originalName) {
        return originalName.toLowerCase();
    }

    public boolean wraps(DataStore ds) {
        return wrapped == ds;
    }

    public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
        // TODO Auto-generated method stub
        return iface != null && iface.isAssignableFrom(this.getClass());
    }

    public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
        // TODO Auto-generated method stub
        try {
            if (iface != null && iface.isAssignableFrom(this.getClass())) {
                return (T) this;
            }
            throw new java.sql.SQLException("Auto-generated unwrap failed; Revisit implementation");
        } catch (Exception e) {
            throw new java.sql.SQLException(e);
        }
    }
}
