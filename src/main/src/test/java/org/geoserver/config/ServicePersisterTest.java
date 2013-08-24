/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class ServicePersisterTest extends GeoServerSystemTestSupport {

    GeoServer geoServer;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        GeoServer geoServer = getGeoServer();
        geoServer.addListener(new ServicePersister(
                (List) Arrays.asList(new ServiceLoader(getResourceLoader())), geoServer));
    }

    @Before
    public void init() {
        geoServer = getGeoServer();
    }

    @Before
    public void removeFooService() {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();
        ServiceInfo s = geoServer.getServiceByName(ws, "foo", ServiceInfo.class);
        if (s != null) {
            geoServer.remove(s);
        }
    }

    @Test
    public void testAddWorkspaceLocalService() throws Exception {
        File dataDirRoot = getTestData().getDataDirectoryRoot();
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();

        ServiceInfo s = geoServer.getFactory().createService();
        s.setName("foo");
        s.setWorkspace(ws);

        File f = new File(dataDirRoot, "workspaces"+"/"+ws.getName()+"/service.xml");
        assertFalse(f.exists());

        geoServer.add(s);
        assertTrue(f.exists());
    }

    @Test
    public void testRemoveWorkspaceLocalService() throws Exception {
        testAddWorkspaceLocalService();

        File dataDirRoot = getTestData().getDataDirectoryRoot();
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();

        File f = new File(dataDirRoot, "workspaces"+"/"+ws.getName()+"/service.xml");
        assertTrue(f.exists());

        ServiceInfo s = geoServer.getServiceByName(ws, "foo", ServiceInfo.class);
        geoServer.remove(s);
        assertFalse(f.exists());
    }
    
    @Test
    public void testReloadWithLocalServices() throws Exception {
        // setup a non default workspace
        WorkspaceInfo ws = getCatalog().getFactory().createWorkspace();
        ws.setName("nonDefault");
        NamespaceInfo ni = getCatalog().getFactory().createNamespace();
        ni.setPrefix("nonDefault");
        ni.setURI("http://www.geoserver.org/nonDefault");
        getCatalog().add(ws);
        getCatalog().add(ni);

        // create a ws specific setting
        SettingsInfo s = geoServer.getFactory().createSettings();
        s.setWorkspace(ws);

        geoServer.add(s);
        
        getGeoServer().reload();
        
    }

    static class ServiceLoader extends XStreamServiceLoader {

        public ServiceLoader(GeoServerResourceLoader resourceLoader) {
            super(resourceLoader, "service");
        }

        @Override
        public Class getServiceClass() {
            return ServiceInfo.class;
        }

        @Override
        protected ServiceInfo createServiceFromScratch(GeoServer gs) {
            return null;
        }
    }
}
