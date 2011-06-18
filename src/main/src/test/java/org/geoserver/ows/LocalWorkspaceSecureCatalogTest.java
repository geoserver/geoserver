package org.geoserver.ows;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.DataAccessManager;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.DataAccessManagerAdapter;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.AbstractAuthorizationTest;

public class LocalWorkspaceSecureCatalogTest extends AbstractAuthorizationTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        populateCatalog();
    }
    
    public void testAccessToLayer() throws Exception {
        DataAccessManager def = buildLegacyAccessManager("wideOpen.properties");
        ResourceAccessManager defAsResourceManager = new DataAccessManagerAdapter(def);
        LocalWorkspaceResourceAccessManager mgr = new LocalWorkspaceResourceAccessManager();
        mgr.setDelegate(defAsResourceManager);
        
        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertNotNull(sc.getLayerByName("topp:states"));
        
        WorkspaceInfo ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertNull(sc.getWorkspaceByName("topp"));
        assertNull(sc.getResourceByName("topp:states", ResourceInfo.class));
        assertNull(sc.getLayerByName("topp:states"));
    }
    
    @Override
    protected void tearDown() throws Exception {
        LocalWorkspace.remove();
    }
}
