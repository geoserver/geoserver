package org.geoserver.ows;

import java.util.Collections;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.DataAccessManager;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.DataAccessManagerAdapter;
import org.geoserver.security.CatalogFilterAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.AbstractAuthorizationTest;

public class LocalWorkspaceSecureCatalogTest extends AbstractAuthorizationTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        populateCatalog();
    }

    CatalogFilterAccessManager setupAccessManager() throws Exception {
        DataAccessManager def = buildLegacyAccessManager("wideOpen.properties");
        ResourceAccessManager defAsResourceManager = new DataAccessManagerAdapter(def);
        CatalogFilterAccessManager mgr = new CatalogFilterAccessManager();
        mgr.setCatalogFilters(Collections.singletonList(new LocalWorkspaceCatalogFilter(catalog)));
        mgr.setDelegate(defAsResourceManager);
        return mgr;
    }
    public void testAccessToLayer() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();
        
        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertNotNull(sc.getLayerByName("topp:states"));
        
        WorkspaceInfo ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertNull(sc.getWorkspaceByName("topp"));
        assertNull(sc.getResourceByName("topp:states", ResourceInfo.class));
        assertNull(sc.getLayerByName("topp:states"));
    }

    public void testAccessToStyle() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertEquals(2, sc.getStyles().size());

        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);
        assertEquals(2, sc.getStyles().size());
        LocalWorkspace.remove();

        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertEquals(1, sc.getStyles().size());
    }

    public void testAccessToLayerGroup() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertEquals(2, sc.getLayerGroups().size());

        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);
        assertEquals(1, sc.getLayerGroups().size());
        LocalWorkspace.remove();

        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertEquals(1, sc.getLayerGroups().size());
        assertEquals("layerGroup", sc.getLayerGroups().get(0).getName());
        LocalWorkspace.remove();
    }
    @Override
    protected void tearDown() throws Exception {
        LocalWorkspace.remove();
    }
}
