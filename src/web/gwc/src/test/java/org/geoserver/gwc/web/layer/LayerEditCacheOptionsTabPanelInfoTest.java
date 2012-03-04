/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import junit.framework.TestCase;

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

public class LayerEditCacheOptionsTabPanelInfoTest extends TestCase {

    LayerEditCacheOptionsTabPanelInfo panelInfo;

    GWCConfig defaults;

    GWC gwc;

    IModel<? extends ResourceInfo> resourceModel;

    LayerInfo layer;

    IModel<LayerInfo> layerModel;

    @Override
    protected void setUp() throws Exception {
        panelInfo = new LayerEditCacheOptionsTabPanelInfo();
        gwc = mock(GWC.class);
        GWC.set(gwc);

        defaults = GWCConfig.getOldDefaults();
        when(gwc.getConfig()).thenReturn(defaults);

        FeatureTypeInfo resource = mock(FeatureTypeInfo.class);
        when(resource.getPrefixedName()).thenReturn("topp:states");

        layer = mock(LayerInfo.class);
        when(layer.getResource()).thenReturn(resource);
        MetadataMap mdm = new MetadataMap();
        when(layer.getMetadata()).thenReturn(mdm);
        resourceModel = new Model<ResourceInfo>(resource);
        layerModel = new Model<LayerInfo>(layer);
    }

    @Override
    protected void tearDown() {
        GWC.set(null);
    }

    public void testCreateOwnModelNew() {
        final boolean isNew = true;

        IModel<GeoServerTileLayerInfo> ownModel;
        ownModel = panelInfo.createOwnModel(resourceModel, layerModel, isNew);
        assertNotNull(ownModel);
        GeoServerTileLayerInfoImpl expected = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        assertEquals(expected, ownModel.getObject());
    }

    public void testCreateOwnModelExisting() {
        final boolean isNew = false;

        IModel<GeoServerTileLayerInfo> ownModel;
        ownModel = panelInfo.createOwnModel(resourceModel, layerModel, isNew);
        assertNotNull(ownModel);
        GeoServerTileLayerInfo expected = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        assertEquals(expected, ownModel.getObject());

        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        expected = new GeoServerTileLayerInfoImpl();
        expected.setEnabled(true);
        when(tileLayer.getInfo()).thenReturn(expected);
        when(gwc.getTileLayer(same(layer))).thenReturn(tileLayer);

        ownModel = panelInfo.createOwnModel(resourceModel, layerModel, isNew);
        assertEquals(expected, ownModel.getObject());
    }
}
