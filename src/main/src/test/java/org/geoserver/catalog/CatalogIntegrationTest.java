package org.geoserver.catalog;

import java.util.List;

import org.geoserver.test.GeoServerTestSupport;

public class CatalogIntegrationTest extends GeoServerTestSupport {

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
