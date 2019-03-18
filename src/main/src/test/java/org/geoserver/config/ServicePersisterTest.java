/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class ServicePersisterTest extends GeoServerSystemTestSupport {

    GeoServer geoServer;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        GeoServer geoServer = getGeoServer();
        geoServer.addListener(
                new ServicePersister(
                        (List) Arrays.asList(new ServiceLoader(getResourceLoader())), geoServer));
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath*:/org/geoserver/config/ServicePersisterTest-applicationContext.xml");
    }

    @Before
    public void init() {
        geoServer = getGeoServer();
    }

    @Before
    public void removeFooService() throws IOException {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();
        ServiceInfo s = geoServer.getServiceByName(ws, "foo", ServiceInfo.class);
        if (s != null) {
            geoServer.remove(s);
        }

        File serviceFile = getDataDirectory().findFile("service.xml");
        if (serviceFile != null) {
            serviceFile.delete();
        }
    }

    @Test
    public void testAddWorkspaceLocalService() throws Exception {
        File dataDirRoot = getTestData().getDataDirectoryRoot();
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();

        ServiceInfo s = geoServer.getFactory().createService();
        s.setName("foo");
        s.setWorkspace(ws);

        File f = new File(dataDirRoot, "workspaces" + "/" + ws.getName() + "/service.xml");
        assertFalse(f.exists());

        geoServer.add(s);
        assertTrue(f.exists());
    }

    @Test
    public void testRemoveWorkspaceLocalService() throws Exception {
        testAddWorkspaceLocalService();

        File dataDirRoot = getTestData().getDataDirectoryRoot();
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();

        File f = new File(dataDirRoot, "workspaces" + "/" + ws.getName() + "/service.xml");
        assertTrue(f.exists());

        Logger logger = Logging.getLogger(GeoServerImpl.class);
        Level level = logger.getLevel();
        try {
            logger.setLevel(Level.OFF);
            ServiceInfo s = geoServer.getServiceByName(ws, "foo", ServiceInfo.class);
            geoServer.remove(s);
            assertFalse(f.exists());
        } finally {
            logger.setLevel(level);
        }
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

    @Test
    public void testLoadGibberish() throws Exception {
        // we should get a log message, but the startup should continue
        File service =
                new File(getDataDirectory().getResourceLoader().getBaseDirectory(), "service.xml");
        FileUtils.writeStringToFile(service, "duDaDa", "UTF-8");
        getGeoServer().reload();
        assertEquals(0, geoServer.getServices().size());
    }

    public static class ServiceLoader extends XStreamServiceLoader {

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
