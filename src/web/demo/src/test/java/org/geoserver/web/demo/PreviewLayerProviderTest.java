/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class PreviewLayerProviderTest extends GeoServerWicketTestSupport {

    @Test
    public void testNonAdvertisedLayer() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        try {
            // now you see me
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, layerId);
            assertNotNull(pl);

            // now you don't!
            layer.setAdvertised(false);
            getCatalog().save(layer);
            pl = getPreviewLayer(provider, layerId);
            assertNull(pl);
        } finally {
            layer.setAdvertised(true);
            getCatalog().save(layer);
        }
    }

    @Test
    public void testSingleLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testSingleLayerGroup");
        group.setMode(LayerGroupInfo.Mode.SINGLE);
        group.getLayers().add(layer);
        group.setTitle("This is the title");
        group.setAbstract("This is the abstract");
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNotNull(pl);
            assertEquals("This is the title", pl.getTitle());
            assertEquals("This is the abstract", pl.getAbstract());
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testDisabledLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testSingleLayerGroup");
        group.setMode(LayerGroupInfo.Mode.SINGLE);
        group.getLayers().add(layer);
        group.setTitle("This is the title");
        group.setAbstract("This is the abstract");
        group.setEnabled(false);
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNull(pl);
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testNotAdvertisedLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testSingleLayerGroup");
        group.setMode(LayerGroupInfo.Mode.SINGLE);
        group.getLayers().add(layer);
        group.setTitle("This is the title");
        group.setAbstract("This is the abstract");
        group.setAdvertised(false);
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNull(pl);
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testOpaqueContainerLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testOpaqueContainerLayerGroup");
        group.setMode(LayerGroupInfo.Mode.OPAQUE_CONTAINER);
        group.getLayers().add(layer);
        group.setTitle("This is the title");
        group.setAbstract("This is the abstract");
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNotNull(pl);
            assertEquals("This is the title", pl.getTitle());
            assertEquals("This is the abstract", pl.getAbstract());
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testWorkspacedLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("cite");

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testWorkspacedLayerGroup");
        group.setMode(LayerGroupInfo.Mode.SINGLE);
        group.setWorkspace(ws);
        group.getLayers().add(layer);
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNotNull(pl);
            assertEquals("cite:testWorkspacedLayerGroup", pl.getName());
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testContainerLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testContainerLayerGroup");
        group.setMode(LayerGroupInfo.Mode.CONTAINER);
        group.getLayers().add(layer);
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNull(pl);
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testNestedContainerLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo containerGroup = getCatalog().getFactory().createLayerGroup();
        containerGroup.setName("testContainerLayerGroup");
        containerGroup.setMode(LayerGroupInfo.Mode.SINGLE);
        containerGroup.getLayers().add(layer);
        getCatalog().add(containerGroup);

        LayerGroupInfo singleGroup = getCatalog().getFactory().createLayerGroup();
        singleGroup.setName("testSingleLayerGroup");
        singleGroup.setMode(LayerGroupInfo.Mode.SINGLE);
        singleGroup.getLayers().add(containerGroup);
        getCatalog().add(singleGroup);

        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            assertNotNull(getPreviewLayer(provider, singleGroup.prefixedName()));
            assertNotNull(getPreviewLayer(provider, layer.prefixedName()));
        } finally {
            getCatalog().remove(singleGroup);
            getCatalog().remove(containerGroup);
        }
    }

    @Test
    public void testKewordsFilterSizeCache() {
        PreviewLayerProvider provider = new PreviewLayerProvider();
        assertEquals(29, provider.size());

        provider.setKeywords(new String[] {"cite"});
        assertEquals(12, provider.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetItems() throws Exception {
        // Ensure that the method getItems is no more called
        PreviewLayerProvider provider = new PreviewLayerProvider();
        provider.getItems();
    }

    private PreviewLayer getPreviewLayer(PreviewLayerProvider provider, String prefixedName) {
        for (PreviewLayer pl : Lists.newArrayList(provider.iterator(0, Integer.MAX_VALUE))) {
            if (pl.getName().equals(prefixedName)) {
                return pl;
            }
        }
        return null;
    }
}
