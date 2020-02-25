/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.google.common.collect.Iterators;
import java.util.Collections;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.CatalogFilterAccessManager;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.AbstractAuthorizationTest;
import org.geoserver.util.PropertyRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

public class LocalWorkspaceSecureCatalogTest extends AbstractAuthorizationTest {

    @Rule
    public PropertyRule inheritance = PropertyRule.system("GEOSERVER_GLOBAL_LAYER_GROUP_INHERIT");

    @Before
    public void setUp() throws Exception {
        LocalWorkspaceCatalogFilter.groupInherit = null;
        super.setUp();
        populateCatalog();
    }

    CatalogFilterAccessManager setupAccessManager() throws Exception {
        ResourceAccessManager defAsResourceManager = buildAccessManager("wideOpen.properties");
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

    @SuppressWarnings({"unchecked"})
    @Test
    public void testAccessToLayerGroup() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertEquals(catalog.getLayerGroups().size(), sc.getLayerGroups().size());

        // all groups in this one or global
        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);
        assertEquals(getWorkspaceAccessibleGroupSize("topp"), sc.getLayerGroups().size());
        LocalWorkspace.remove();

        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertEquals(getWorkspaceAccessibleGroupSize("nurc"), sc.getLayerGroups().size());
        assertEquals("layerGroup", sc.getLayerGroups().get(0).getName());
        LocalWorkspace.remove();
    }

    private long getWorkspaceAccessibleGroupSize(String workspaceName) {
        return catalog.getLayerGroups()
                .stream()
                .filter(
                        lg ->
                                lg.getWorkspace() == null
                                        || workspaceName.equals(lg.getWorkspace().getName()))
                .count();
    }

    @Test
    public void testAccessToLayerGroupNoInheritance() throws Exception {
        CatalogFilterAccessManager mgr = setupAccessManager();
        inheritance.setValue("false");

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, mgr) {};
        assertThat(sc.getLayerGroups(), hasItem(equalTo(layerGroupGlobal)));
        assertThat(sc.getLayerGroups(), hasItem(equalTo(layerGroupTopp)));
        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);
        assertThat(sc.getLayerGroups(), not(hasItem(equalTo(layerGroupGlobal))));
        assertThat(sc.getLayerGroups(), hasItem(equalTo(layerGroupTopp)));
        LocalWorkspace.remove();

        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        assertThat(sc.getLayerGroups(), not(hasItem(equalTo(layerGroupGlobal))));
        assertThat(sc.getLayerGroups(), not(hasItem(equalTo(layerGroupTopp))));
        LocalWorkspace.remove();
    }

    @Test
    public void testAccessToStyleAsIterator() throws Exception {
        // Getting the access manager
        CatalogFilterAccessManager mgr = setupAccessManager();

        // Defining a SecureCatalog with a user which is not admin
        SecureCatalogImpl sc =
                new SecureCatalogImpl(catalog, mgr) {
                    @Override
                    protected boolean isAdmin(Authentication authentication) {
                        return false;
                    }
                };
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);

        // Get the iterator on the styles
        CloseableIterator<StyleInfo> styles = sc.list(StyleInfo.class, Filter.INCLUDE);
        int size = Iterators.size(styles);
        assertEquals(2, size);

        // Setting the workspace "topp" and repeating the test
        WorkspaceInfo ws = sc.getWorkspaceByName("topp");
        LocalWorkspace.set(ws);

        styles = sc.list(StyleInfo.class, Filter.INCLUDE);
        size = Iterators.size(styles);
        assertEquals(2, size);
        LocalWorkspace.remove();

        // Setting the workspace "nurc" and repeating the test
        ws = sc.getWorkspaceByName("nurc");
        LocalWorkspace.set(ws);
        styles = sc.list(StyleInfo.class, Filter.INCLUDE);
        size = Iterators.size(styles);
        assertEquals(1, size);
    }

    @After
    public void tearDown() throws Exception {
        LocalWorkspace.remove();
    }
}
