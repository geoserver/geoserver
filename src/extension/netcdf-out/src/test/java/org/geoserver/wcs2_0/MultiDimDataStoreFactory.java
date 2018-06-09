/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;

public class MultiDimDataStoreFactory implements DataStoreFactorySpi {

    @Override
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        return new MultiDimDataStore((String) params.get("ParentLocation"));
    }

    @Override
    public String getDisplayName() {
        return "MultiDim";
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        File dir = new File((String) params.get("ParentLocation"));

        if (dir.exists()) {
            throw new IOException(dir + " already exists");
        }

        return new MultiDimDataStore((String) params.get("ParentLocation"));
    }

    @Override
    public String getDescription() {
        return "Data store using the dummy catalog as backend.";
    }

    @Override
    public Param[] getParametersInfo() {
        return new DataStoreFactorySpi.Param[] {new Param("ParentLocation")};
    }

    @Override
    public boolean canProcess(Map<String, Serializable> map) {
        File dir = new File((String) map.get("ParentLocation"));
        return dir.exists();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<RenderingHints.Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }
}
