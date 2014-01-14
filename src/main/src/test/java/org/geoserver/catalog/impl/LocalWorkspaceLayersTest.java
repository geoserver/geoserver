/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.*;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;

public class LocalWorkspaceLayersTest extends GeoServerSystemTestSupport {

    Catalog catalog;

    @Before
    public void setUpInternal() {
        catalog = getCatalog();
    }

    @Test
    public void testGroupLayerInWorkspace() {
        WorkspaceInfo workspace = catalog.getWorkspaceByName("sf");
        WorkspaceInfo workspace2 = catalog.getWorkspaceByName("cite");
        CatalogFactory factory = catalog.getFactory();
        LayerGroupInfo globalGroup = factory.createLayerGroup();
        globalGroup.setName("globalGroup");
        globalGroup.setWorkspace(workspace2);
        globalGroup.getLayers().add(catalog.getLayerByName("Lakes"));
        catalog.add(globalGroup);

        LayerGroupInfo localGroup = factory.createLayerGroup();
        localGroup.setName("localGroup");
        localGroup.setWorkspace(workspace);
        localGroup.getLayers().add(catalog.getLayerByName("GenericEntity"));
        catalog.add(localGroup);
        String localName = localGroup.prefixedName();
        assertEquals("sf:localGroup", localName);

        assertEquals(2, catalog.getLayerGroups().size());

        LocalWorkspace.set(workspace2);
        assertNull(catalog.getLayerGroupByName("localGroup"));
        LocalWorkspace.remove();

        LocalWorkspace.set(workspace);
        assertNotNull(catalog.getLayerGroupByName("localGroup"));
        assertEquals(1, catalog.getLayerGroups().size());
        assertEquals("localGroup", catalog.getLayerGroupByName("localGroup").prefixedName());
        LocalWorkspace.remove();
    }

    @Test
    public void testLayersInLocalWorkspace() {
        List<LayerInfo> layers = catalog.getLayers();

        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        WorkspaceInfo cite = catalog.getWorkspaceByName("cite");

        CatalogFactory factory = catalog.getFactory();
        
        DataStoreInfo citeStore = factory.createDataStore();
        citeStore.setEnabled(true);
        citeStore.setName("globalStore");
        citeStore.setWorkspace(cite);
        catalog.add(citeStore);
        
        FeatureTypeInfo citeFeatureType = factory.createFeatureType();
        citeFeatureType.setName("citeLayer");
        citeFeatureType.setStore(citeStore);
        citeFeatureType.setNamespace(catalog.getNamespaceByPrefix("cite"));
        catalog.add(citeFeatureType);
        
        LayerInfo citeLayer = factory.createLayer();
        citeLayer.setResource(citeFeatureType);
        citeLayer.setEnabled(true);
        //citeLayer.setName("citeLayer");
        catalog.add(citeLayer);

        assertNotNull(catalog.getLayerByName("citeLayer"));
        assertEquals("cite:citeLayer", catalog.getLayerByName("citeLayer").prefixedName());

        DataStoreInfo sfStore = factory.createDataStore();
        sfStore.setEnabled(true);
        sfStore.setName("localStore");
        sfStore.setWorkspace(sf);
        catalog.add(sfStore);
        
        FeatureTypeInfo sfFeatureType = factory.createFeatureType();
        sfFeatureType.setName("sfLayer");
        sfFeatureType.setStore(sfStore);
        sfFeatureType.setNamespace(catalog.getNamespaceByPrefix("sf"));
        catalog.add(sfFeatureType);

        LayerInfo sfLayer = factory.createLayer();
        sfLayer.setResource(sfFeatureType);
        sfLayer.setEnabled(true);
        //sfLayer.setName("sfLayer");
        catalog.add(sfLayer);

        assertNotNull(catalog.getLayerByName("citeLayer"));
        assertNotNull(catalog.getLayerByName("sfLayer"));

        LocalWorkspace.set(sf);
        assertNull(catalog.getLayerByName("citeLayer"));
        assertNotNull(catalog.getLayerByName("sfLayer"));
        assertEquals("sfLayer", catalog.getLayerByName("sfLayer").prefixedName());
        LocalWorkspace.remove();

        LocalWorkspace.set(cite);
        assertNull(catalog.getLayerByName("sfLayer"));
        assertNotNull(catalog.getLayerByName("citeLayer"));
        assertEquals("citeLayer", catalog.getLayerByName("citeLayer").prefixedName());
        LocalWorkspace.remove();
    }
}
