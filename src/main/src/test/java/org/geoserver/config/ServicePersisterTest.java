package org.geoserver.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerTestSupport;

public class ServicePersisterTest extends GeoServerTestSupport {

    GeoServer geoServer;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        geoServer = getGeoServer();
        geoServer.addListener(new ServicePersister(
            (List) Arrays.asList(new ServiceLoader(getResourceLoader())), geoServer));
    }

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
