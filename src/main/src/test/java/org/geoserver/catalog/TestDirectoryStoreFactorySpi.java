/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.data.memory.MemoryDataStore;

/** A fake store that returns nothing, used to similate the FGB store interaction with directory data store */
public class TestDirectoryStoreFactorySpi implements DataStoreFactorySpi {

    public static final Param URL_PARAM = new Param("url", URL.class, "A file or directory", true, null);

    @Override
    public DataStore createDataStore(Map<String, ?> params) throws IOException {
        return new TestDirectoryStore();
    }

    @Override
    public String getDisplayName() {
        return "Test store accepting a simple URL";
    }

    @Override
    public String getDescription() {
        return "A test factory with no parameters and one fixed data type";
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[] {URL_PARAM};
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public DataStore createNewDataStore(Map<String, ?> params) throws IOException {
        return createDataStore(params);
    }

    /**
     * Empty subclass, just to have a clearer name on test failures, e.g., "failed to cast to TestDirectoryStore" is
     * better than "failed to case to MemoryDataStore", e.g., it would lead immediately to this file. In case you're
     * seeing this, it's likely that you created a DataStoreInfo without setting the "type" attribute to the desired
     * data store factory displayName.
     */
    public static class TestDirectoryStore extends MemoryDataStore {}
}
