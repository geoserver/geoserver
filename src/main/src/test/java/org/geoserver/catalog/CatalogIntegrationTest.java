package org.geoserver.catalog;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.List;

import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class CatalogIntegrationTest extends GeoServerSystemTestSupport {

    @Test
    public void testWorkspaceRemoveAndReadd() {
        // remove all workspaces
        Catalog catalog = getCatalog();
        NamespaceInfo defaultNamespace = catalog.getDefaultNamespace();
        WorkspaceInfo defaultWs = catalog.getDefaultWorkspace();
        List<WorkspaceInfo> workspaces = catalog.getWorkspaces();
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        for (WorkspaceInfo ws : workspaces) {
            visitor.visit(ws);
        }
        assertEquals(0, catalog.getWorkspaces().size());
        assertEquals(0, catalog.getNamespaces().size());
        
        // add back one (this would NPE)
        catalog.add(defaultNamespace);
        catalog.add(defaultWs);
        assertEquals(1, catalog.getWorkspaces().size());
        assertEquals(1, catalog.getNamespaces().size());
        
        // get back by name (this would NPE too)
        assertNotNull(catalog.getNamespaceByURI(defaultNamespace.getURI()));
    }
}
