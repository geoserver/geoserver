/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.security;

import java.io.IOException;

import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.ReadOnlyDataStore;
import org.geotools.data.DataStore;
import org.geotools.data.VersioningDataStore;

/**
 * Read only wrapper around a versioning data store
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class ReadOnlyVersioningDataStore extends ReadOnlyDataStore implements VersioningDataStore {

    ReadOnlyVersioningDataStore(DataStore delegate, WrapperPolicy policy) {
        super(delegate, policy);
    }

    public boolean isVersioned(String typeName) throws IOException {
        return ((VersioningDataStore) delegate).isVersioned(typeName);
    }

    public void setVersioned(String typeName, boolean versioned, String author, String message)
            throws IOException {
        throw notifyUnsupportedOperation();
    }
}
