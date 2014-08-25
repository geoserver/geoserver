/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.layer.TileLayer;
import org.junit.Before;
import org.junit.Test;

public class LayerCacheOptionsTabPanelTest extends GeoServerWicketTestSupport {

    private IModel<LayerInfo> layerModel;

    private GeoServerTileLayerInfoModel tileLayerModel;

    @Before
    public void setUpInternal() throws Exception {
        LayerInfo layerInfo = getCatalog().getLayerByName(getLayerId(MockData.BUILDINGS));
        assertTrue(CatalogConfiguration.isLayerExposable(layerInfo));
        GeoServerTileLayer tileLayer = GWC.get().getTileLayer(layerInfo);
        assertNotNull(tileLayer);
        layerModel = new Model<LayerInfo>(layerInfo);
        tileLayerModel = new GeoServerTileLayerInfoModel(tileLayer.getInfo(), false);
    }

    @Test
    public void testPageLoad() {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = -5907648151984337786L;

            public Component buildComponent(final String id) {
                return new LayerCacheOptionsTabPanel(id, layerModel, tileLayerModel);
            }
        }));

        tester.assertComponent("form:panel", LayerCacheOptionsTabPanel.class);
        tester.assertComponent("form:panel:tileLayerEditor", GeoServerTileLayerEditor.class);
    }

    @Test
    public void testPageLoadForGeometrylessLayer() {
        LayerInfo geometryless = getCatalog().getLayerByName(
                super.getLayerId(MockData.GEOMETRYLESS));
        
        assertFalse(CatalogConfiguration.isLayerExposable(geometryless));
        assertNull(GWC.get().getTileLayer(geometryless));

        layerModel = new Model<LayerInfo>(geometryless);
        final GWCConfig saneDefaults = GWC.get().getConfig().saneConfig();
        GeoServerTileLayerInfoImpl tileLayerInfo = TileLayerInfoUtil.loadOrCreate(geometryless,
                saneDefaults);
        tileLayerModel = new GeoServerTileLayerInfoModel(tileLayerInfo, false);

        tester.startPage(new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = -5907648151984337786L;

            public Component buildComponent(final String id) {
                return new LayerCacheOptionsTabPanel(id, layerModel, tileLayerModel);
            }
        }));

        tester.assertComponent("form:panel", LayerCacheOptionsTabPanel.class);
        //Label instead of GeoServerTileLayerEditor
        tester.assertComponent("form:panel:tileLayerEditor", Label.class);
    }

    @Test
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

    @Test
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

    @Test
    public void testRemoveExisting() {

        LayerInfo layerInfo = getCatalog().getLayerByName(getLayerId(MockData.LAKES));
        GeoServerTileLayer tileLayer = GWC.get().getTileLayer(layerInfo);
        assertNotNull(tileLayer);
        layerModel = new Model<LayerInfo>(layerInfo);
        tileLayerModel = new GeoServerTileLayerInfoModel(tileLayer.getInfo(), false);

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

    @Test
    public void testAddNullFilter() {
        // Create a form page for the LayerCacheOptionsTabPanel component
        FormTestPage page = new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = -5907648151984337786L;

            public Component buildComponent(final String id) {
                return new LayerCacheOptionsTabPanel(id, layerModel, tileLayerModel);
            }
        });
        // Start the page
        tester.startPage(page);
        // Ensure the GeoServerTileLayerEditor is rendered
        tester.assertComponent("form:panel:tileLayerEditor", GeoServerTileLayerEditor.class);
        // Create new form tester for the final submit
        FormTester form = tester.newFormTester("form");
        // Click on the addFilter button withou setting any filter
        tester.executeAjaxEvent(
                "form:panel:tileLayerEditor:container:configs:parameterFilters:addFilter",
                "onclick");
        // Ensure that the Component is rendered again
        tester.assertComponent("form:panel:tileLayerEditor", GeoServerTileLayerEditor.class);
        // Ensure that an Error message has been thrown
        tester.assertErrorMessages(new String[] { "Filter should not be empty" });
        // Save the changes
        form.submit();
        // Check no exception has been thrown
        tester.assertNoErrorMessage();
    }
}
