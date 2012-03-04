/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import java.util.Arrays;

import junit.framework.Test;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.layer.TileLayer;

public class LayerCacheOptionsTabPanelTest extends GeoServerWicketTestSupport {

    private IModel<LayerInfo> layerModel;

    private GeoServerTileLayerInfoModel tileLayerModel;

    public static Test suite() {
        return new OneTimeTestSetup(new LayerCacheOptionsTabPanelTest());
    }

    @Override
    protected void setUpInternal() throws Exception {
        LayerInfo layerInfo = getCatalog().getLayers().get(0);
        GeoServerTileLayer tileLayer = GWC.get().getTileLayer(layerInfo);
        assertNotNull(tileLayer);
        layerModel = new Model<LayerInfo>(layerInfo);
        tileLayerModel = new GeoServerTileLayerInfoModel(tileLayer.getInfo(), false);
    }

    public void testPageLoad() {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = -5907648151984337786L;

            public Component buildComponent(final String id) {
                return new LayerCacheOptionsTabPanel(id, layerModel, tileLayerModel);
            }
        }));

        tester.assertComponent("form:panel", LayerCacheOptionsTabPanel.class);
    }

    public void testSaveExisting() {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = -6705646666953650890L;

            public Component buildComponent(final String id) {
                return new LayerCacheOptionsTabPanel(id, layerModel, tileLayerModel);
            }
        }));

        tester.assertComponent("form:panel", LayerCacheOptionsTabPanel.class);

        GeoServerTileLayerInfo info = tileLayerModel.getObject();
        info.setEnabled(!info.isEnabled());

        LayerCacheOptionsTabPanel panel = (LayerCacheOptionsTabPanel) tester
                .getComponentFromLastRenderedPage("form:panel");
        panel.save();

        TileLayer tileLayer = GWC.get().getTileLayerByName(info.getName());
        GeoServerTileLayerInfo actual = ((GeoServerTileLayer) tileLayer).getInfo();
        assertEquals(info.isEnabled(), actual.isEnabled());
    }

    public void testSaveNew() {
        GWC mediator = GWC.get();
        mediator.removeTileLayers(Arrays.asList(tileLayerModel.getObject().getName()));

        assertNull(mediator.getTileLayer(layerModel.getObject()));

        GeoServerTileLayerInfo newInfo = TileLayerInfoUtil.loadOrCreate(layerModel.getObject(),
                mediator.getConfig());

        tileLayerModel = new GeoServerTileLayerInfoModel(newInfo, true);

        tester.startPage(new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = -6705646666953650890L;

            public Component buildComponent(final String id) {
                return new LayerCacheOptionsTabPanel(id, layerModel, tileLayerModel);
            }
        }));

        tester.assertComponent("form:panel", LayerCacheOptionsTabPanel.class);

        LayerCacheOptionsTabPanel panel = (LayerCacheOptionsTabPanel) tester
                .getComponentFromLastRenderedPage("form:panel");

        panel.save();

        assertNotNull(mediator.getTileLayer(layerModel.getObject()));
    }

    public void testRemoveExisting() {

        GWC mediator = GWC.get();

        assertNotNull(mediator.getTileLayer(layerModel.getObject()));

        tester.startPage(new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = -6705646666953650890L;

            public Component buildComponent(final String id) {
                return new LayerCacheOptionsTabPanel(id, layerModel, tileLayerModel);
            }
        }));

        tester.assertComponent("form:panel", LayerCacheOptionsTabPanel.class);

        // mimic what the editor does to remove a tile layer associated with a layer info
        // print(tester.getComponentFromLastRenderedPage("form:panel"), true, true);
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("panel:tileLayerEditor:createTileLayer", false);

        formTester.submit();

        LayerCacheOptionsTabPanel panel = (LayerCacheOptionsTabPanel) tester
                .getComponentFromLastRenderedPage("form:panel");

        panel.save();

        assertNull(mediator.getTileLayer(layerModel.getObject()));
    }
}
