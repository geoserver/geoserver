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
package org.geootols.dggs.clickhouse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.dggs.DGGSFactoryFinder;
import org.geotools.dggs.DGGSInstance;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;

/**
 * Factory for {@link org.geotools.dggs.gstore.DGGSStore} based on ClickHouse storage . TODO:
 * generalize this so that it can take DGGS parameters as well.
 */
// TODO: add a limit to the complexity of queries the store is willing to accept? And suggest the
// user to switch to a lower resolution instead. Though maybe this ought to be done at the
// service level, and propagated down to the store as a Hint?
public class ClickHouseDGGStoreFactory implements DataStoreFactorySpi {

    ClickHouseJDBCDataStoreFactory delegate = new ClickHouseJDBCDataStoreFactory();

    /** parameter for database type */
    // TODO: find some better way to separate this from the DGGSGeometryStore?
    public static final Param DGGS_FACTORY_ID =
            new Param(
                    "dggs_id",
                    String.class,
                    "DGGS Factory identifier, e.g., H3 or rHEALPix",
                    true,
                    null);

    @Override
    public DataStore createNewDataStore(Map<String, ?> params) throws IOException {
        return createDataStore(params);
    }

    @Override
    public DataStore createDataStore(Map<String, ?> params) throws IOException {
        // setup the JDBC data store based on Clickhouse
        Map<String, Object> delegateParams = new HashMap<>(params);
        delegateParams.put(JDBCDataStoreFactory.DBTYPE.key, delegate.getDatabaseID());
        delegateParams.put(
                JDBCDataStoreFactory.SCHEMA.key, params.get(JDBCDataStoreFactory.DATABASE.key));
        JDBCDataStore jdbcStore = delegate.createDataStore(delegateParams);

        // setup the DGGS instance
        String factoryId = (String) DGGS_FACTORY_ID.lookUp(params);
        DGGSInstance instance = DGGSFactoryFinder.createInstance(factoryId, params);

        return new ClickHouseDGGSDataStore(instance, jdbcStore);
    }

    @Override
    public String getDisplayName() {
        return "ClickHouse DGGS integration";
    }

    @Override
    public String getDescription() {
        return "ClickHouse DGGS integration";
    }

    @Override
    public Param[] getParametersInfo() {
        Stream<Param> delegateParams =
                Stream.of(delegate.getParametersInfo())
                        .filter(
                                p ->
                                        !JDBCDataStoreFactory.DBTYPE.key.equals(p.key)
                                                && !JDBCDataStoreFactory.SCHEMA.key.equals(p.key));
        return Stream.concat(Stream.of(DGGS_FACTORY_ID), delegateParams).toArray(n -> new Param[n]);
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable()
                && DGGSFactoryFinder.getExtensionFactories().findAny().isPresent();
    }
}
