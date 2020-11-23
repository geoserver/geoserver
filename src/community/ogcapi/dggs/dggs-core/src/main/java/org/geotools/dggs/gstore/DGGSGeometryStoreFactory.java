/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.gstore;

import java.io.IOException;
import java.util.Map;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.dggs.DGGSFactory;
import org.geotools.dggs.DGGSFactoryFinder;
import org.geotools.dggs.DGGSInstance;

/**
 * Creates a {@link DGGSGeometryStore} based solely on a {@link DGGSFactory} identifier, the
 * parameter map can contain initialization params for the
 */
public class DGGSGeometryStoreFactory implements DataStoreFactorySpi {

    /** parameter for database type */
    public static final Param DGGS_FACTORY_ID =
            new Param(
                    "dggs_factory_id",
                    String.class,
                    "DGGS Factory identifier, e.g., H3 or rHEALPix",
                    true,
                    null);

    /** parameter for namespace of the datastore */
    public static final Param NAMESPACE =
            new Param("namespace", String.class, "Namespace prefix", false);

    @Override
    public DataStore createDataStore(Map<String, ?> params) throws IOException {
        String factoryId = (String) DGGS_FACTORY_ID.lookUp(params);
        DGGSInstance instance = DGGSFactoryFinder.createInstance(factoryId, params);
        DGGSGeometryStore store = new DGGSGeometryStore(instance);

        String namespace = (String) NAMESPACE.lookUp(params);
        if (namespace != null) {
            store.setNamespaceURI(namespace);
        }

        return store;
    }

    @Override
    public DataStore createNewDataStore(Map<String, ?> params) throws IOException {
        return createDataStore(params);
    }

    @Override
    public String getDisplayName() {
        return "DGGS Geometry Store";
    }

    @Override
    public String getDescription() {
        return "Store returning DGGS zones, with no data associated (pure geometric description)";
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[] {NAMESPACE, DGGS_FACTORY_ID};
    }

    @Override
    public boolean isAvailable() {
        return DGGSFactoryFinder.getExtensionFactories().findFirst().isPresent();
    }
}
