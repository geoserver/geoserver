/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.data.test.MockData.PONDS;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;
import static org.geoserver.web.GeoServerWicketTestSupport.tester;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.layer.TileLayer;
import org.hamcrest.Matchers;
import org.junit.Test;

/** @author prushforth */
public class MapMLLayerConfigurationPanelTest extends GeoServerWicketTestSupport {
    static QName MOSAIC = new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX);
    GeoServerTileLayer tileLayer;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // we don't want any of the defaults
        testData.setUpSecurity();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(PONDS, getCatalog());
        Map<SystemTestData.LayerProperty, Object> props = new HashMap<>();

        testData.addRasterLayer(
                MOSAIC, "raster-filter-test.zip", null, props, SystemTestData.class, getCatalog());
        testData.addVectorLayer(BASIC_POLYGONS, getCatalog());
    }

    @Test
    public void testMapMLPanel() {
        // get a test layer and instantiate the model
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        Model<LayerInfo> model = new Model<>(layer);
        FormTestPage page =
                new FormTestPage(
                        (ComponentBuilder) id -> new MapMLLayerConfigurationPanel(id, model));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        // check that the attributes dropdown is available
        tester.assertComponent("form:panel:featurecaptionattributes", ListMultipleChoice.class);
        tester.assertComponent("form:panel:mime", DropDownChoice.class);
        // check that the mime type pick list is available as expected with vector data
        tester.assertEnabled("form:panel:mime");
        // check that the "useFeatures" checkbox is enabled as expected with vector data
        tester.assertEnabled("form:panel:useFeatures");
        FormTester ft = tester.newFormTester("form");
        tester.assertModelValue("form:panel:licenseTitle", null);
        tester.assertModelValue("form:panel:licenseLink", null);
        tester.assertModelValue("form:panel:useTiles", null);
        tester.assertModelValue("form:panel:useFeatures", null);
        tester.assertModelValue("form:panel:dimension", null);
        tester.assertModelValue("form:panel:featurecaptionattributes", null);
        tester.assertModelValue("form:panel:featureCaptionTemplate", null);

        ft.setValue("panel:licenseTitle", "A Fake Title");
        ft.setValue("panel:licenseLink", "https://example.org/mapml");
        ft.setValue("panel:useTiles", true);
        ft.setValue("panel:useFeatures", true);
        // no dimension set up yet should not be able to select one...
        try {
            ft.select("panel:dimension", 0);
            fail("No dimension should be set up yet.");
        } catch (Exception e) {

        }

        // select the "NAME" attribute from Ponds, map to featurecaption in MapML
        ft.select("panel:featurecaptionattributes", 2);
        ft.setValue("panel:featureCaptionTemplate", "This is a ${test}");

        ft.submit();
        tester.assertModelValue("form:panel:licenseTitle", "A Fake Title");
        tester.assertModelValue("form:panel:licenseLink", "https://example.org/mapml");
        tester.assertModelValue("form:panel:useTiles", true);
        tester.assertModelValue("form:panel:useFeatures", true);
        //        tester.assertModelValue("form:panel:featurecaptionattributes", "[NAME]");
        tester.assertModelValue("form:panel:featureCaptionTemplate", "This is a ${test}");
    }

    @Test
    public void testMapMLPanelWithRasterData() {
        // get a test layer and instantiate the model
        final LayerInfo layer = getCatalog().getLayerByName(MOSAIC.getLocalPart());
        Model<LayerInfo> model = new Model<>(layer);
        FormTestPage page =
                new FormTestPage(
                        (ComponentBuilder) id -> new MapMLLayerConfigurationPanel(id, model));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        // check that the "attributes" (works with raster dimensions) dropdown is available
        tester.assertComponent("form:panel:featurecaptionattributes", ListMultipleChoice.class);
        tester.assertComponent("form:panel:mime", DropDownChoice.class);
        // check that the "useFeatures" checkbox is disabled as expected with raster data
        tester.assertDisabled("form:panel:useFeatures");
        FormTester ft = tester.newFormTester("form");
        tester.assertModelValue("form:panel:featurecaptionattributes", null);
        tester.assertModelValue("form:panel:licenseTitle", null);
        tester.assertModelValue("form:panel:licenseLink", null);
        tester.assertModelValue("form:panel:useTiles", null);
        tester.assertModelValue("form:panel:useFeatures", null);
        tester.assertModelValue("form:panel:dimension", null);
        tester.assertModelValue("form:panel:featurecaptionattributes", null);
        tester.assertModelValue("form:panel:featureCaptionTemplate", null);

        ft.setValue("panel:licenseTitle", "A Fake Title");
        ft.setValue("panel:licenseLink", "https://example.org/mapml");
        ft.setValue("panel:useTiles", true);
        ft.setValue("panel:featureCaptionTemplate", "This is a ${test}");

        // no dimension set up yet should not be able to select one...
        try {
            ft.select("panel:dimension", 0);
            fail("No dimension should be set up yet.");
        } catch (Exception e) {

        }
        // select the "Blue band" from Mosaic, map to featurecaption in MapML
        ft.select("panel:featurecaptionattributes", 2);
        ft.setValue("panel:featureCaptionTemplate", "This is the ${BLUE_BAND}");

        ft.submit();
        tester.assertModelValue("form:panel:licenseTitle", "A Fake Title");
        tester.assertModelValue("form:panel:licenseLink", "https://example.org/mapml");
        tester.assertModelValue("form:panel:useTiles", true);
        //      tester.assertModelValue("form:panel:featurecaptionattributes", "[BLUE_BAND]");
        tester.assertModelValue("form:panel:featureCaptionTemplate", "This is the ${BLUE_BAND}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMapMLMime() {
        // get a test layer and instantiate the model
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        layer.getResource().getMetadata().put(MapMLConstants.MAPML_USE_FEATURES, true);
        Model<LayerInfo> model = new Model<>(layer);

        FormTestPage page =
                new FormTestPage(
                        (ComponentBuilder) id -> new MapMLLayerConfigurationPanel(id, model));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);

        tester.assertComponent("form:panel:mime", DropDownChoice.class);
        // check that the mime type pick list is disabled when mapML useFeatures is enabled
        tester.assertDisabled("form:panel:mime");

        // check that the "mime" checkbox is disabled as expected when mapML useFeatures is enabled
        tester.assertDisabled("form:panel:mime");
        layer.getResource().getMetadata().put(MapMLConstants.MAPML_USE_FEATURES, false);
        Model<LayerInfo> model2 = new Model<>(layer);
        page =
                new FormTestPage(
                        (ComponentBuilder) id -> new MapMLLayerConfigurationPanel(id, model2));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);
        // check that the "mime" checkbox is enabled as expected when mapML useFeatures is disabled
        tester.assertEnabled("form:panel:mime");
        DropDownChoice<String> dropDownChoice =
                (DropDownChoice) tester.getComponentFromLastRenderedPage("form:panel:mime");
        assertThat(
                dropDownChoice.getChoices(),
                Matchers.containsInAnyOrder(
                        "image/png; mode=8bit",
                        "image/vnd.jpeg-png",
                        "image/jpeg",
                        "image/vnd.jpeg-png8",
                        "image/png",
                        "image/png8"));
        GWC mediator = GWC.get();

        final String layerName = getLayerId(BASIC_POLYGONS);
        LayerInfo layerInfo = getCatalog().getLayerByName(layerName);
        assertNotNull(layerInfo);
        layerInfo.getResource().getMetadata().put(MapMLConstants.MAPML_USE_FEATURES, false);
        layerInfo.getResource().getMetadata().put(MAPML_USE_TILES, true);

        TileLayer tileLayer = mediator.getTileLayerByName(layerName);
        assertNotNull(tileLayer);
        assertTrue(tileLayer.isEnabled());

        Model<LayerInfo> modelTile = new Model<>(layerInfo);

        FormTestPage pageTile =
                new FormTestPage(
                        (ComponentBuilder) id -> new MapMLLayerConfigurationPanel(id, modelTile));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(pageTile);
        DropDownChoice<String> dropDownChoiceTile =
                (DropDownChoice) tester.getComponentFromLastRenderedPage("form:panel:mime");
        assertThat(
                dropDownChoiceTile.getChoices(),
                Matchers.containsInAnyOrder("image/jpeg", "image/png"));
    }
}
