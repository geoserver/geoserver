/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geoserver.jdbcconfig.internal;

import java.lang.reflect.Proxy;

import junit.framework.TestCase;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;

/**
 * @author groldan
 * 
 */
public class ConfigDatabaseTest extends TestCase {

    private JdbcConfigTestSupport testSupport;

    private ConfigDatabase database;

    @Override
    protected void setUp() throws Exception {
        testSupport = new JdbcConfigTestSupport();
        testSupport.setUp();
        database = testSupport.getDatabase();
    }

    @Override
    protected void tearDown() throws Exception {
        database.dispose();
        testSupport.tearDown();
    }

    public void testAdd() throws Exception {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        try {
            database.add(ws);
            fail("Expected NPE on null id");
        } catch (NullPointerException e) {
            assertEquals("Object has no id", e.getMessage());
        }
        ws.setId("wsid");
        ws.setName("ws1");

        WorkspaceInfo addedWs = database.add(ws);
        assertNotNull(addedWs);
        assertTrue(addedWs instanceof Proxy);
        assertEquals(ws, addedWs);

        DataStoreInfo addedDs = addDataStore(ws);
        assertNotNull(addedDs);
    }

    private DataStoreInfo addDataStore(WorkspaceInfo ws) {
        Catalog catalog = database.getCatalog();
        DataStoreInfoImpl ds = new DataStoreInfoImpl(catalog);
        ds.setWorkspace(ws);
        ds.setId("ds1");
        ds.getConnectionParameters().put("param1", "value1");
        ds.getConnectionParameters().put("param2", "value2");
        ds.setName("data store one");
        ds.setDescription("data store description one");
        ds.setEnabled(true);
        ds.setType("Foo");

        DataStoreInfo addedDs = database.add(ds);
        return addedDs;
    }

    public void testModifyWorkspace() throws Exception {
        WorkspaceInfo ws = addWorkspace();
        ws.setName("newName");
        testSaved(ws);
    }

    private WorkspaceInfo addWorkspace() {
        WorkspaceInfo ws = new WorkspaceInfoImpl();
        ((WorkspaceInfoImpl) ws).setId("wsid");
        ws.setName("ws1");

        ws = database.add(ws);
        return ws;
    }

    /**
     * @param info
     */
    private void testSaved(Info info) {
        Info saved = database.save(info);
        assertNotSame(info, saved);
        if (info instanceof DataStoreInfo) {
            assertEquals(((DataStoreInfo) info).getWorkspace(),
                    ((DataStoreInfo) saved).getWorkspace());
        }
        assertEquals(info, saved);
    }
}
