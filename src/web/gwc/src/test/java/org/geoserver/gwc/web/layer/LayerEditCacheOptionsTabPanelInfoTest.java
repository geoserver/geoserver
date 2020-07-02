/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LayerEditCacheOptionsTabPanelInfoTest {

    LayerEditCacheOptionsTabPanelInfo panelInfo;

    GWCConfig defaults;

    GWC gwc;

    IModel<? extends ResourceInfo> resourceModel;

    LayerInfo layer;

    IModel<LayerInfo> layerModel;

    @Before
    public void setUpInternal() throws Exception {
        panelInfo = new LayerEditCacheOptionsTabPanelInfo();
        gwc = mock(GWC.class);
        GWC.set(gwc);

        defaults = GWCConfig.getOldDefaults();
        when(gwc.getConfig()).thenReturn(defaults);

        FeatureTypeInfo resource = mock(FeatureTypeInfo.class);
        when(resource.prefixedName()).thenReturn("topp:states");

        layer = mock(LayerInfo.class);
        when(layer.getResource()).thenReturn(resource);
        MetadataMap mdm = new MetadataMap();
        when(layer.getMetadata()).thenReturn(mdm);
        resourceModel = new Model<ResourceInfo>(resource);
        layerModel = new Model<LayerInfo>(layer);
    }

    @After
    public void tearDown() {
        GWC.set(null);
    }

    @Test
    public void testCreateOwnModelNew() {
        final boolean isNew = true;

        IModel<GeoServerTileLayerInfo> ownModel;
        ownModel = panelInfo.createOwnModel(layerModel, isNew);
        assertNotNull(ownModel);
        GeoServerTileLayerInfoImpl expected = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        assertEquals(expected, ownModel.getObject());
    }

    @Test
    public void testCreateOwnModelExisting() {

        final boolean isNew = false;

        IModel<GeoServerTileLayerInfo> ownModel;
        ownModel = panelInfo.createOwnModel(layerModel, isNew);
        assertNotNull(ownModel);
        GeoServerTileLayerInfo expected = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        assertEquals(expected, ownModel.getObject());

        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        expected = new GeoServerTileLayerInfoImpl();
        expected.setEnabled(true);
        when(tileLayer.getInfo()).thenReturn(expected);
        when(gwc.getTileLayer(same(layer))).thenReturn(tileLayer);

        ownModel = panelInfo.createOwnModel(layerModel, isNew);
        assertEquals(expected, ownModel.getObject());
    }

    @Test
    public void testCreateOwnModelExistingWithEnabledFalse() {

        // test that if a layer is existing and has enable caching set to false
        // enable value is not replaced with true
        final boolean isNew = false;

        IModel<GeoServerTileLayerInfo> ownModel;
        ownModel = panelInfo.createOwnModel(layerModel, isNew);
        assertNotNull(ownModel);
        GeoServerTileLayerInfo expected = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        assertEquals(expected, ownModel.getObject());

        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        expected = new GeoServerTileLayerInfoImpl();
        expected.setEnabled(false);
        when(tileLayer.getInfo()).thenReturn(expected);
        when(gwc.getTileLayer(same(layer))).thenReturn(tileLayer);

        ownModel = panelInfo.createOwnModel(layerModel, isNew);
        assertEquals(expected, ownModel.getObject());
    }
}
