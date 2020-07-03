/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.WMSInterpolation;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.MockData;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.web.publish.StylesModel;
import org.geoserver.wms.web.publish.WMSLayerConfig;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("serial")
public class WMSLayerConfigTest extends GeoServerWicketTestSupport {

    @Before
    public void resetPondStyle() {
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(MockData.PONDS.getLocalPart());
        style.setWorkspace(null);
        catalog.save(style);
    }

    @Test
    public void testExisting() {
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, new Model(layer));
                            }
                        });
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:styles:defaultStyle", DropDownChoice.class);

        // check selecting something else works
        StyleInfo target = ((List<StyleInfo>) new StylesModel().getObject()).get(0);
        FormTester ft = tester.newFormTester("form");
        ft.select("panel:styles:defaultStyle", 0);
        ft.submit();
        tester.assertModelValue("form:panel:styles:defaultStyle", target);
        assertFalse(cascadedControlsVisible(tester));
    }

    @Test
    public void testNew() {
        final LayerInfo layer = getCatalog().getFactory().createLayer();
        layer.setResource(getCatalog().getFactory().createFeatureType());
        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, new Model(layer));
                            }
                        });
        Component layerConfig = page.get("form:panel:styles:defaultStyle");

        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:styles:defaultStyle", DropDownChoice.class);

        // check submitting like this will create errors, there is no selection
        tester.submitForm("form");

        assertTrue(layerConfig.getFeedbackMessages().hasMessage(FeedbackMessage.ERROR));

        // now set something and check there are no messages this time
        page.getSession().getFeedbackMessages().clear();
        FormTester ft = tester.newFormTester("form");
        ft.select("panel:styles:defaultStyle", 0);
        ft.submit();
        assertFalse(layerConfig.getFeedbackMessages().hasMessage(FeedbackMessage.ERROR));
        assertFalse(cascadedControlsVisible(tester));
    }

    @Test
    public void testLegendGraphicURL() throws Exception {
        // force style into ponds workspace
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(MockData.PONDS.getLocalPart());
        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.PONDS.getPrefix());
        style.setWorkspace(ws);
        catalog.save(style);

        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, new Model(layer));
                            }
                        });
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.debugComponentTrees();

        Image img =
                (Image)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:styles:defaultStyleLegendGraphic");
        assertNotNull(img);
        assertEquals(1, img.getBehaviors().size());
        assertTrue(img.getBehaviors().get(0) instanceof AttributeModifier);

        AttributeModifier mod = (AttributeModifier) img.getBehaviors().get(0);
        assertTrue(mod.toString().contains("wms?REQUEST=GetLegendGraphic"));
        assertTrue(mod.toString().contains("style=cite:Ponds"));
        assertFalse(cascadedControlsVisible(tester));
    }

    @Test
    public void testInterpolationDropDown() {
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        final Model<LayerInfo> layerModel = new Model<LayerInfo>(layer);

        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, layerModel);
                            }
                        });

        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:defaultInterpolationMethod", DropDownChoice.class);

        // By default, no interpolation method is specified
        FormTester ft = tester.newFormTester("form");
        ft.submit();

        tester.assertModelValue("form:panel:defaultInterpolationMethod", null);

        // Select Bicubic interpolation method
        ft = tester.newFormTester("form");
        ft.select("panel:defaultInterpolationMethod", 2);
        ft.submit();

        tester.assertModelValue("form:panel:defaultInterpolationMethod", WMSInterpolation.Bicubic);
        assertFalse(cascadedControlsVisible(tester));
    }

    @Test
    public void testWMSCascadeSettings() throws Exception {
        MockHttpClient wms11Client = new MockHttpClient();
        URL wms11BaseURL = new URL(TestHttpClientProvider.MOCKSERVER + "/wms11");
        URL capsDocument = WMSLayerConfigTest.class.getResource("caps111.xml");
        wms11Client.expectGet(
                new URL(wms11BaseURL + "?service=WMS&request=GetCapabilities&version=1.1.1"),
                new MockHttpResponse(capsDocument, "text/xml"));
        String caps = wms11BaseURL + "?service=WMS&request=GetCapabilities&version=1.1.1";
        TestHttpClientProvider.bind(wms11Client, caps);

        // setup the WMS layer
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        WMSStoreInfo store = cb.buildWMSStore("mock-wms-store-110");
        getCatalog().add(store);
        cb.setStore(store);
        store.setCapabilitiesURL(caps);
        WMSLayerInfo wmsLayer = cb.buildWMSLayer("roads");
        wmsLayer.setName("roads");
        wmsLayer.reset();
        // keep track of remote style count before loading the model
        int remoteStyleCount = wmsLayer.getAllAvailableRemoteStyles().size();
        getCatalog().add(wmsLayer);
        LayerInfo gsLayer = cb.buildLayer(wmsLayer);
        getCatalog().add(gsLayer);

        final Model<LayerInfo> layerModel = new Model<LayerInfo>(gsLayer);

        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, layerModel);
                            }
                        });

        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);

        // asserting Remote Style UI fields
        tester.assertModelValue(
                "form:panel:remotestyles:remoteStylesDropDown", wmsLayer.getForcedRemoteStyle());
        tester.assertModelValue(
                "form:panel:remotestyles:extraRemoteStyles",
                new HashSet<String>(wmsLayer.remoteStyles()));
        // make sure remote styles on are not duplicated when loaded on page
        assertTrue(wmsLayer.remoteStyles().size() == remoteStyleCount);
        // asserting Remote Style UI fields
        tester.assertModelValue(
                "form:panel:remoteformats:remoteFormatsDropDown", wmsLayer.getPreferredFormat());
        tester.assertModelValue(
                "form:panel:remoteformats:remoteFormatsPalette",
                new HashSet<String>(wmsLayer.availableFormats()));
        tester.assertVisible("form:panel:metaDataCheckBoxContainer");

        // min max scale UI fields
        tester.assertVisible("form:panel:scaleDenominatorContainer:minScale");
        tester.assertVisible("form:panel:scaleDenominatorContainer:maxScale");

        // validation check, setting min scale above max
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:scaleDenominatorContainer:minScale", "100");
        ft.setValue("panel:scaleDenominatorContainer:maxScale", "1");
        ft.submit();
        // there should be an error
        tester.assertErrorMessages("Minimum Scale cannot be greater than Maximum Scale");
        assertTrue(cascadedControlsVisible(tester));
    }

    private boolean cascadedControlsVisible(WicketTester tester) {
        // check visibility of all cscaded controls
        return tester.getComponentFromLastRenderedPage("form:panel:remotestyles") != null
                && tester.getComponentFromLastRenderedPage("form:panel:remoteformats") != null
                && tester.getComponentFromLastRenderedPage("form:panel:scaleDenominatorContainer")
                        != null
                && tester.getComponentFromLastRenderedPage("form:panel:metaDataCheckBoxContainer")
                        != null;
    }

    @Test
    public void testWMSCascadeSettingsLegacyBean() throws Exception {
        // this test asserts that the wmslayer settings do not break
        // when user has switched from 2.16.2 and less version

        // Create a Legacy Resource usong < 2.16.2 WMSLayerInfo XML without fields storing new
        // settings
        XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();

        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wmsLayer>\n"
                        + "   <id>WMSLayerInfoImpl-622caab0:16ff63f5f7a:-7ffc</id>\n"
                        + "   <name>legacy_roads</name>\n"
                        + "   <nativeName>roads</nativeName>\n"
                        + "   <title>Legacy</title>\n"
                        + "   <description>Legacy</description>\n"
                        + "   <abstract>Legacy</abstract>\n"
                        + "   <keywords>\n"
                        + "      <string>census</string>\n"
                        + "      <string>united</string>\n"
                        + "      <string>boundaries</string>\n"
                        + "      <string>state</string>\n"
                        + "      <string>states</string>\n"
                        + "   </keywords>\n"
                        + "   <nativeCRS>GEOGCS[\"WGS 84\", &#xD;\n"
                        + "  DATUM[\"World Geodetic System 1984\", &#xD;\n"
                        + "    SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], &#xD;\n"
                        + "    AUTHORITY[\"EPSG\",\"6326\"]], &#xD;\n"
                        + "  PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], &#xD;\n"
                        + "  UNIT[\"degree\", 0.017453292519943295], &#xD;\n"
                        + "  AXIS[\"Geodetic longitude\", EAST], &#xD;\n"
                        + "  AXIS[\"Geodetic latitude\", NORTH], &#xD;\n"
                        + "  AUTHORITY[\"EPSG\",\"4326\"]]</nativeCRS>\n"
                        + "   <srs>EPSG:4326</srs>\n"
                        + "   <nativeBoundingBox>\n"
                        + "      <minx>-124.73142200000001</minx>\n"
                        + "      <maxx>-66.969849</maxx>\n"
                        + "      <miny>24.955967</miny>\n"
                        + "      <maxy>49.371735</maxy>\n"
                        + "      <crs>EPSG:4326</crs>\n"
                        + "   </nativeBoundingBox>\n"
                        + "   <latLonBoundingBox>\n"
                        + "      <minx>-124.731422</minx>\n"
                        + "      <maxx>-66.969849</maxx>\n"
                        + "      <miny>24.955967</miny>\n"
                        + "      <maxy>49.371735</maxy>\n"
                        + "      <crs>EPSG:4326</crs>\n"
                        + "   </latLonBoundingBox>\n"
                        + "   <projectionPolicy>FORCE_DECLARED</projectionPolicy>\n"
                        + "   <enabled>true</enabled>\n"
                        + "   <serviceConfiguration>false</serviceConfiguration>\n"
                        + "</wmsLayer>";

        WMSLayerInfoImpl legacyWmsLayerInfo =
                (WMSLayerInfoImpl)
                        persister.load(
                                new ByteArrayInputStream(xml.getBytes()), WMSLayerInfo.class);

        MockHttpClient wms11Client = new MockHttpClient();
        URL wms11BaseURL = new URL(TestHttpClientProvider.MOCKSERVER + "/wms11");
        URL capsDocument = WMSLayerConfigTest.class.getResource("caps111.xml");
        wms11Client.expectGet(
                new URL(wms11BaseURL + "?service=WMS&request=GetCapabilities&version=1.1.1"),
                new MockHttpResponse(capsDocument, "text/xml"));
        String caps = wms11BaseURL + "?service=WMS&request=GetCapabilities&version=1.1.1";
        TestHttpClientProvider.bind(wms11Client, caps);

        // setup the WMS layer
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        WMSStoreInfo store = cb.buildWMSStore("mock-wms-legacy-store-110");
        getCatalog().add(store);
        cb.setStore(store);
        store.setCapabilitiesURL(caps);
        WMSLayerInfo wmsLayer = cb.buildWMSLayer("roads");
        // copy values into Legacy Resource
        legacyWmsLayerInfo.setNamespace(wmsLayer.getNamespace());
        legacyWmsLayerInfo.setCatalog(wmsLayer.getCatalog());
        legacyWmsLayerInfo.setStore(wmsLayer.getStore());

        getCatalog().add(legacyWmsLayerInfo);
        // build layer using legacy resource
        LayerInfo gsLayer = cb.buildLayer(legacyWmsLayerInfo);
        getCatalog().add(gsLayer);

        final Model<LayerInfo> layerModel = new Model<LayerInfo>(gsLayer);

        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, layerModel);
                            }
                        });

        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        // should not complain about no error messages
        tester.assertNoErrorMessage();

        // assert defaults
        // asserting Remote Style UI fields is set to empty
        tester.assertModelValue(
                "form:panel:remotestyles:remoteStylesDropDown", wmsLayer.getForcedRemoteStyle());
        // asserting preffered format is set to png
        tester.assertModelValue(
                "form:panel:remoteformats:remoteFormatsDropDown", wmsLayer.getPreferredFormat());

        // NOW assert if drop down are showing the defaults as selected on GUI
        DropDownChoice<String> remotStyles =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:remotestyles:remoteStylesDropDown");
        DropDownChoice<String> remoteformats =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:remoteformats:remoteFormatsDropDown");

        assertFalse(remoteformats.getChoicesModel().getObject().isEmpty());
        assertFalse(remotStyles.getChoicesModel().getObject().isEmpty());

        Palette<String> remoteFormatsPalette =
                (Palette<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:remoteformats:remoteFormatsPalette");
        Palette<String> extraRemoteStyles =
                (Palette<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:remotestyles:extraRemoteStyles");

        // assert palettes have populated choices
        assertFalse(remoteFormatsPalette.getChoices().isEmpty());
        assertFalse(extraRemoteStyles.getChoices().isEmpty());

        // assert no choice is selected, its for user to select them on will
        assertNull(remoteFormatsPalette.getConvertedInput());
        assertNull(extraRemoteStyles.getConvertedInput());
    }
}
