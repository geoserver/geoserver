/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.data.test.MockData.PONDS;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.layer.TileLayer;
import org.hamcrest.Matchers;
import org.junit.Test;

public class MapMLLayerGroupConfigurationPanelTest extends GeoServerWicketTestSupport {
    static QName MOSAIC = new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX);

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

        testData.addRasterLayer(MOSAIC, "raster-filter-test.zip", null, props, SystemTestData.class, getCatalog());
        testData.addVectorLayer(BASIC_POLYGONS, getCatalog());
    }

    private LayerGroupInfo createTestLayerGroup(String name, boolean includeRaster) {
        LayerGroupInfo layerGroup = getCatalog().getFactory().createLayerGroup();
        layerGroup.setName(name);
        layerGroup.setTitle(name + " Title");
        layerGroup.setAbstract(name + " Abstract");

        // Add vector layer
        LayerInfo pondsLayer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        layerGroup.getLayers().add(pondsLayer);
        layerGroup.getStyles().add(null);

        LayerInfo polygonsLayer = getCatalog().getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        layerGroup.getLayers().add(polygonsLayer);
        layerGroup.getStyles().add(null);

        if (includeRaster) {
            // Add raster layer
            LayerInfo mosaicLayer = getCatalog().getLayerByName(MOSAIC.getLocalPart());
            layerGroup.getLayers().add(mosaicLayer);
            layerGroup.getStyles().add(null);
        }

        getCatalog().add(layerGroup);
        // Return the proxied version from the catalog
        return getCatalog().getLayerGroupByName(name);
    }

    @Test
    public void testMapMLPanel() {
        // create a test layer group with vector layers
        LayerGroupInfo layerGroup = createTestLayerGroup("testVectorGroup", false);
        Model<LayerGroupInfo> model = new Model<>(layerGroup);
        FormTestPage page = new FormTestPage((ComponentBuilder) id -> new MapMLLayerGroupConfigurationPanel(id, model));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        // check that the mime dropdown is available
        tester.assertComponent("form:panel:mime", DropDownChoice.class);
        // check that the mime type pick list is available as expected
        tester.assertEnabled("form:panel:mime");
        // check that the "useFeatures" checkbox is enabled as expected
        tester.assertEnabled("form:panel:useFeatures");
        // check that the "useTiles" checkbox is enabled
        tester.assertEnabled("form:panel:useTiles");
        // check that the "useMultiExtents" checkbox is enabled
        tester.assertEnabled("form:panel:useMultiExtents");

        FormTester ft = tester.newFormTester("form");
        tester.assertModelValue("form:panel:licenseTitle", null);
        tester.assertModelValue("form:panel:licenseLink", null);
        tester.assertModelValue("form:panel:useTiles", null);
        tester.assertModelValue("form:panel:useFeatures", null);
        tester.assertModelValue("form:panel:useMultiExtents", null);

        ft.setValue("panel:licenseTitle", "A Fake Layer Group Title");
        ft.setValue("panel:licenseLink", "https://example.org/mapml/layergroup");
        ft.setValue("panel:useTiles", true);
        ft.setValue("panel:useFeatures", true);
        ft.setValue("panel:useMultiExtents", true);

        ft.submit();
        tester.assertModelValue("form:panel:licenseTitle", "A Fake Layer Group Title");
        tester.assertModelValue("form:panel:licenseLink", "https://example.org/mapml/layergroup");
        tester.assertModelValue("form:panel:useTiles", true);
        tester.assertModelValue("form:panel:useFeatures", true);
        tester.assertModelValue("form:panel:useMultiExtents", true);

        // Clean up
        getCatalog().remove(layerGroup);
    }

    @Test
    public void testMapMLPanelWithMixedData() {
        // create a test layer group with both vector and raster layers
        LayerGroupInfo layerGroup = createTestLayerGroup("testMixedGroup", true);
        Model<LayerGroupInfo> model = new Model<>(layerGroup);
        FormTestPage page = new FormTestPage((ComponentBuilder) id -> new MapMLLayerGroupConfigurationPanel(id, model));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        // check that the mime dropdown is available
        tester.assertComponent("form:panel:mime", DropDownChoice.class);
        // check that the "useFeatures" checkbox is enabled
        tester.assertEnabled("form:panel:useFeatures");
        // check that the "useTiles" checkbox is enabled
        tester.assertEnabled("form:panel:useTiles");
        // check that the "useMultiExtents" checkbox is enabled
        tester.assertEnabled("form:panel:useMultiExtents");

        FormTester ft = tester.newFormTester("form");
        tester.assertModelValue("form:panel:licenseTitle", null);
        tester.assertModelValue("form:panel:licenseLink", null);
        tester.assertModelValue("form:panel:useTiles", null);
        tester.assertModelValue("form:panel:useFeatures", null);
        tester.assertModelValue("form:panel:useMultiExtents", null);

        ft.setValue("panel:licenseTitle", "A Mixed Data Layer Group");
        ft.setValue("panel:licenseLink", "https://example.org/mapml/mixed");
        ft.setValue("panel:useTiles", true);
        ft.setValue("panel:useMultiExtents", true);

        ft.submit();
        tester.assertModelValue("form:panel:licenseTitle", "A Mixed Data Layer Group");
        tester.assertModelValue("form:panel:licenseLink", "https://example.org/mapml/mixed");
        tester.assertModelValue("form:panel:useTiles", true);
        tester.assertModelValue("form:panel:useMultiExtents", true);

        // Clean up
        getCatalog().remove(layerGroup);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMapMLMime() {
        // this test is a refactor of the corresponding test in MapMLLayerConfigurationPanelTest
        // but NOTE that that test relies on state from an earlier test, so this
        // test must itself set the use features/tiles and test accordingly
        // create a test layer group
        LayerGroupInfo layerGroup = createTestLayerGroup("testMimeGroup", false);
        layerGroup.getMetadata().put(MapMLConstants.MAPML_USE_FEATURES, true);
        Model<LayerGroupInfo> model = new Model<>(layerGroup);

        FormTestPage page = new FormTestPage((ComponentBuilder) id -> new MapMLLayerGroupConfigurationPanel(id, model));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);

        tester.assertComponent("form:panel:mime", DropDownChoice.class);
        // check that the mime type pick list is disabled when mapML useFeatures is enabled
        tester.assertDisabled("form:panel:mime");

        layerGroup.getMetadata().put(MapMLConstants.MAPML_USE_FEATURES, false);
        Model<LayerGroupInfo> model2 = new Model<>(layerGroup);
        page = new FormTestPage((ComponentBuilder) id -> new MapMLLayerGroupConfigurationPanel(id, model2));
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

        // Test with tiles enabled
        layerGroup.getMetadata().put(MapMLConstants.MAPML_USE_FEATURES, false);
        layerGroup.getMetadata().put(MAPML_USE_TILES, true);

        TileLayer tileLayer = mediator.getTileLayerByName(layerGroup.getName());
        assertNotNull(tileLayer);
        assertTrue(tileLayer.isEnabled());
        Model<LayerGroupInfo> modelTile = new Model<>(layerGroup);

        FormTestPage pageTile =
                new FormTestPage((ComponentBuilder) id -> new MapMLLayerGroupConfigurationPanel(id, modelTile));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(pageTile);
        DropDownChoice<String> dropDownChoiceTile =
                (DropDownChoice) tester.getComponentFromLastRenderedPage("form:panel:mime");
        assertThat(dropDownChoiceTile.getChoices(), Matchers.containsInAnyOrder("image/jpeg", "image/png"));

        // Clean up
        getCatalog().remove(layerGroup);
    }
}
