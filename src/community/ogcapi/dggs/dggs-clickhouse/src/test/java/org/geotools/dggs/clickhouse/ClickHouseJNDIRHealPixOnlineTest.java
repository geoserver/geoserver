/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2024, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.dggs.clickhouse;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.dggs.DGGSFactoryFinder;
import org.geotools.jdbc.JDBCJNDIDataStoreFactory;
import org.geotools.util.factory.GeoTools;
import org.mockito.Mockito;

@SuppressWarnings("ErrorProne.BanJNDI")
public class ClickHouseJNDIRHealPixOnlineTest extends ClickHouseRHealPixOnlineTest {

    @Override
    protected ClickHouseDGGSDataStore getDataStore() throws Exception {
        String dggsId = getDGGSId();
        if (!DGGSFactoryFinder.getFactory(dggsId).isPresent()) {
            throw new Exception(dggsId + " is not present, skipping the test");
        }

        ClickHouseDGGStoreFactory factory = new ClickHouseDGGStoreFactory();
        @SuppressWarnings("unchecked")
        BasicDataSource dataSource = (BasicDataSource) factory.createDataSource((Map) fixture);
        MockInitialDirContextFactory.setDataSource(dataSource);
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MockInitialDirContextFactory.class.getName());
        try {
            GeoTools.clearInitialContext();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> params = new HashMap<>();
        params.put(JDBCJNDIDataStoreFactory.DBTYPE.key, "clickhouse");
        params.put(JDBCJNDIDataStoreFactory.JNDI_REFNAME.key, "ds");
        params.put(ClickHouseDGGStoreFactory.DGGS_FACTORY_ID.key, dggsId);

        return (ClickHouseDGGSDataStore) DataStoreFinder.getDataStore(params);
    }

    public static class MockInitialDirContextFactory implements InitialContextFactory {

        private Context mockContext = null;
        private static BasicDataSource dataSource;

        public static void setDataSource(BasicDataSource dataSource) {
            MockInitialDirContextFactory.dataSource = dataSource;
        }

        @Override
        // JDK API, we cannot do anything about it
        @SuppressWarnings({"PMD.ReplaceHashtableWithMap", "ErrorProne.BanJNDI"})
        public Context getInitialContext(Hashtable environment) throws NamingException {
            mockContext = Mockito.mock(Context.class);
            Mockito.when(mockContext.lookup("ds")).thenReturn(dataSource);
            return mockContext;
        }
    }
}
