package org.geoserver.ogcapi.dggs;

import java.io.IOException;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.ReadOnlyDataStore;
import org.geotools.data.DataStore;
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
