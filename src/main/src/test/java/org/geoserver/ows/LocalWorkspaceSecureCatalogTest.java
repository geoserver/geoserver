/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.junit.Assert.*;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalWorkspaceSecureCatalogTest extends AbstractAuthorizationTest {

    @Before
    public void setUp() throws Exception {
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
    @Test
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

    @Test
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

    @Test
    public void testAccessToLayerGroup() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertEquals(3, sc.getLayerGroups().size());

        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);
        assertEquals(3, sc.getLayerGroups().size());
        LocalWorkspace.remove();

        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertEquals(1, sc.getLayerGroups().size());
        assertEquals("layerGroup", sc.getLayerGroups().get(0).getName());
        LocalWorkspace.remove();
    }
    
    @After
    public void tearDown() throws Exception {
        LocalWorkspace.remove();
    }
}
