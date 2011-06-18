package org.geoserver.catalog.impl;

import java.util.List;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.test.GeoServerTestSupport;

public class WorkspaceNamespaceConstencyTest extends GeoServerTestSupport {
    
    public void testChangeWorkspace() {
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();
        ws.setName(ws.getName() + "abcd");
        getCatalog().save(ws);
        
        // check the corresponding namespace has been modified
        NamespaceInfo ns = getCatalog().getDefaultNamespace();
        assertNotNull(ns);
        assertEquals(ws.getName(), ns.getPrefix());
    }
    
    public void testChangeNamespace() {
        NamespaceInfo ns = getCatalog().getDefaultNamespace();
        ns.setPrefix(ns.getPrefix() + "abcd");
        getCatalog().save(ns);
        
        // check the corresponding namespace has been modified
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();
        assertNotNull(ws);
        assertEquals(ns.getPrefix(), ws.getName());
    }
    
    public void testChangeDefaultWorkspace() {
        List<WorkspaceInfo> workspaces = getCatalog().getWorkspaces();
        workspaces.remove(getCatalog().getDefaultWorkspace());
        WorkspaceInfo newDefault = workspaces.get(0);
        getCatalog().setDefaultWorkspace(newDefault);
        
        // check the default namespace changed accordingly
        NamespaceInfo ns = getCatalog().getDefaultNamespace();
        assertNotNull(ns);
        assertEquals(newDefault.getName(), ns.getPrefix());
    }
    
    public void testChangeDefaultNamespace() {
        List<NamespaceInfo> namespaces = getCatalog().getNamespaces();
        namespaces.remove(getCatalog().getDefaultNamespace());
        NamespaceInfo newDefault = namespaces.get(0);
        getCatalog().setDefaultNamespace(newDefault);
        
        // check the default namespace changed accordingly
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();
        assertNotNull(ws);
        assertEquals(newDefault.getName(), ws.getName());
    }
    
    public void testChangeNamespaceURI() {
        // gran a workspace that has stores in it
        WorkspaceInfo ws = getCatalog().getStores(DataStoreInfo.class).get(0).getWorkspace();
        // alter the namespace uri
        NamespaceInfo ns = getCatalog().getNamespaceByPrefix(ws.getName());
        ns.setURI("http://www.geoserver.org/newNamespace");
        getCatalog().save(ns);
        
        List<DataStoreInfo> stores = getCatalog().getDataStoresByWorkspace(ws);
        assertTrue(stores.size() > 0);
        for (DataStoreInfo ds : stores) {
            String nsURI = (String) ds.getConnectionParameters().get("namespace");
            if(nsURI != null) {
                assertEquals(ns.getURI(), nsURI);
            }
        }
    }
}
