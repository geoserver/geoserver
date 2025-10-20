/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import java.io.IOException;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.ReadOnlyDataStore;
import org.geotools.api.data.DataStore;
import org.geotools.dggs.gstore.DGGSFeatureSource;
import org.geotools.dggs.gstore.DGGSStore;

class ReadOnlyDGGSStore extends ReadOnlyDataStore implements DGGSStore {

    protected ReadOnlyDGGSStore(DataStore delegate, WrapperPolicy policy) {
        super(delegate, policy);
    }

    @Override
    public DGGSFeatureSource getDGGSFeatureSource(String typeName) throws IOException {
        return ((DGGSStore) delegate).getDGGSFeatureSource(typeName);
    }
}
