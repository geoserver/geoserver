/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MapPreviewPageTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setupIAULayers(true, true);
    }

    @Before
    public void setOutputPaths() {
        GeoServerApplication.get().getDebugSettings().setComponentPathAttributeName("wicketPath");
    }

    @Test
    public void testValues() throws Exception {
        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);
    }

    @Test
    public void testLayerGroupNamesPrefixed() throws Exception {
        Catalog cat = getCatalog();

        LayerGroupInfo lg = cat.getFactory().createLayerGroup();
        lg.setName("foo");
        lg.setWorkspace(cat.getWorkspaceByName("sf"));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.PRIMITIVEGEOFEATURE)));
        new CatalogBuilder(cat).calculateLayerGroupBounds(lg);

        cat.add(lg);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        // move to next page
        tester.clickLink("table:navigatorBottom:navigator:next", true);

        @SuppressWarnings("unchecked")
        DataView<Component> data = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        boolean exists = false;
        for (org.apache.wicket.Component datum : data) {
            MarkupContainer c = (MarkupContainer) datum;
            Label l = (Label) c.get("itemProperties:2:component");
            String model = l.getDefaultModelObjectAsString();
            if ("sf:foo".equals(model)) {
                exists = true;
            }
        }

        assertTrue(exists);
    }

    @Test
    @Ignore
    public void testLayerNamesPrefixed() throws Exception {
        Catalog cat = getCatalog();

        LayerInfo ly = cat.getLayerByName(getLayerId(MockData.STREAMS));

        assertNotNull(ly);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        // move to next page
        tester.clickLink("table:navigatorBottom:navigator:next", true);

        @SuppressWarnings("unchecked")
        DataView<Component> data = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        boolean exists = false;
        for (org.apache.wicket.Component datum : data) {
            MarkupContainer c = (MarkupContainer) datum;
            Label l = (Label) c.get("itemProperties:1:component");
            if (getLayerId(MockData.STREAMS).equals(l.getDefaultModelObjectAsString())) {
                exists = true;
            }
        }

        assertTrue(exists);
    }

    @Test
    public void testMaxNumberOfFeaturesForPreview() throws Exception {

        GeoServer geoserver = getGeoServer();
        WFSInfo wfsInfo = geoserver.getService(WFSInfo.class);

        int maxFeatures = 100;
        wfsInfo.setMaxNumberOfFeaturesForPreview(maxFeatures);
        geoserver.save(wfsInfo);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        assertMaxFeaturesInData(tester.getLastResponseAsString(), maxFeatures);

        maxFeatures = 0;
        wfsInfo.setMaxNumberOfFeaturesForPreview(maxFeatures);
        geoserver.save(wfsInfo);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        assertMaxFeaturesInData(tester.getLastResponseAsString(), maxFeatures);
    }

    /**
     * Max features is handled by a Javascript building the URLs client, so we need to check its code to make sure the
     * maxFeatures parameter is being set correctly.
     */
    private void assertMaxFeaturesInData(String html, int maxFeatures) {
        String PREFIX = "var maxFeature = '";
        String END = "';";
        if (maxFeatures > 0) {
            assertThat(html, containsString(PREFIX + "&maxFeatures=" + maxFeatures + END));
        } else {
            assertThat(html, containsString(PREFIX + END));
        }
    }

    @Test
    public void testWfsOutputFormatValueUrlEncoding() {
        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        Label optionLabel = (Label) tester.getComponentFromLastRenderedPage(
                "table:listContainer:items:4:itemProperties:4:component:menu:wfs:wfsFormats:3");
        assertEquals("GML3.2", optionLabel.getDefaultModelObjectAsString());
        for (Behavior b : optionLabel.getBehaviors()) {
            if (b instanceof AttributeModifier) {
                AttributeModifier am = (AttributeModifier) b;
                String url = am.toString();
                assertFalse(url.contains("gml+xml"));
                assertTrue(url.contains("gml%2Bxml"));
                break;
            }
        }
    }

    @Test
    public void testNameURLEncoding() {
        Catalog catalog = getCatalog();
        FeatureTypeInfo ft = catalog.getFeatureTypeByName("cite:Lakes");
        ft.setName("Lakes + a plus");
        catalog.save(ft);
        try {

            tester.startPage(MapPreviewPage.class);
            tester.assertRenderedPage(MapPreviewPage.class);
            // print(tester.getLastRenderedPage(), true, true, true);

            @SuppressWarnings("unchecked")
            DataView<Component> data = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

            boolean exists = false;
            String path = null;
            for (org.apache.wicket.Component datum : data) {
                MarkupContainer c = (MarkupContainer) datum;
                Label l = (Label) c.get("itemProperties:2:component");
                String model = l.getDefaultModelObjectAsString();
                if ("cite:Lakes + a plus".equals(model)) {
                    exists = true;
                    path = c.getPageRelativePath();

                    // check visible links
                    ExternalLink olLink = (ExternalLink)
                            c.get("itemProperties:3:component:commonFormat:0").getDefaultModelObject();
                    ExternalLink gmlLink = (ExternalLink)
                            c.get("itemProperties:3:component:commonFormat:1").getDefaultModelObject();
                    ExternalLink kmlLink = (ExternalLink)
                            c.get("itemProperties:3:component:commonFormat:2").getDefaultModelObject();

                    assertEquals(
                            "http://localhost/context/cite/wms?service=WMS&amp;version=1.1.0&amp;"
                                    + "request=GetMap&amp;layers=cite%3ALakes%20%2B%20a%20plus&amp;"
                                    + "bbox=-180.0%2C-90.0%2C180.0%2C90.0&amp;width=768&amp;"
                                    + "height=384&amp;srs=EPSG%3A4326&amp;styles=&amp;"
                                    + "format=application/openlayers",
                            olLink.getDefaultModelObjectAsString());
                    assertThat(
                            gmlLink.getDefaultModelObjectAsString(),
                            containsString("http://localhost/context/cite/ows?service=WFS&amp;version=1"
                                    + ".0.0&amp;request=GetFeature&amp;"
                                    + "typeName=cite%3ALakes%20%2B%20a%20plus"));
                    assertEquals(
                            kmlLink.getDefaultModelObjectAsString(),
                            "http://localhost/context/cite/wms/kml?layers=cite%3ALakes%20%2B%20a%20plus");

                    // check formats
                    RepeatingView wmsFormats = (RepeatingView) c.get("itemProperties:4:component:menu:wms:wmsFormats");
                    if (wmsFormats != null) {
                        assertFormat(wmsFormats, "JPEG-PNG", true);
                    }

                    RepeatingView wfsFormats = (RepeatingView) c.get("itemProperties:4:component:menu:wfs:wfsFormats");
                    if (wfsFormats != null) {
                        assertFormat(wfsFormats, "CSV", true);
                        assertFormat(wfsFormats, "text/csv", true);
                    }
                }
            }
            assertTrue("Could not find layer with expected name", exists);

            String html = tester.getLastResponseAsString();
            TagTester menuTester = TagTester.createTagByAttribute(
                    html, "wicketPath", path.replace(":", "_") + "_itemProperties_4_component_menu");
            assertThat(
                    menuTester.getAttribute("wmsLink"),
                    containsString(
                            "http://localhost/context/cite/wms?service=WMS&version=1.1.0&request=GetMap&layers=cite%3ALakes%20%2B%20a%20plus&bbox=-180.0%2C-90.0%2C180.0%2C90.0&width=768&height=384&srs=EPSG%3A4326&styles="));
            assertThat(
                    menuTester.getAttribute("wfsLink"),
                    containsString(
                            "http://localhost/context/cite/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=cite%3ALakes%20%2B%20a%20plus"));
        } finally {
            ft.setName("Lines");
            catalog.save(ft);
        }
    }

    private void assertFormat(RepeatingView view, String format, boolean expected) {
        Boolean found = view.visitChildren(Label.class, (IVisitor<Label, Boolean>) (label, visit) -> {
            if (label.getDefaultModelObjectAsString().contains(format)) {
                visit.stop(true);
            }
        });
        assertEquals(format, expected, found == null ? false : found);
    }

    /** Test for layer group service support check */
    @Test
    public void testHasServiceSupport() throws Exception {
        Catalog cat = getCatalog();
        LayerGroupInfo lg = cat.getFactory().createLayerGroup();
        lg.setName("linkgroup");
        lg.setWorkspace(cat.getWorkspaceByName("sf"));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.PRIMITIVEGEOFEATURE)));
        new CatalogBuilder(cat).calculateLayerGroupBounds(lg);
        cat.add(lg);
        PreviewLayer layer = new PreviewLayer(lg);
        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);
        assertTrue(layer.hasServiceSupport("WMS"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMarsPreview() throws Exception {
        Catalog cat = getCatalog();
        WorkspaceInfo ws = cat.getWorkspaceByName(SystemTestData.IAU_PREFIX);
        LocalWorkspace.set(ws);
        try {
            tester.startPage(MapPreviewPage.class);
            print(tester.getLastRenderedPage(), true, true, true);

            DataView<Component> data = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

            for (org.apache.wicket.Component datum : data) {
                MarkupContainer c = (MarkupContainer) datum;

                // check OL preview link
                ExternalLink link = (ExternalLink)
                        c.get("itemProperties:3:component:commonFormat:0").getDefaultModelObject();
                String location = link.getDefaultModelObjectAsString();
                assertThat(location, containsString("srs=IAU%3A49900"));
            }

        } finally {
            LocalWorkspace.remove();
        }
    }

    @Test
    public void testCachingImages() throws Exception {
        // test that the "?antiCache=###" query string is not appended to the img src
        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);
        tester.clickLink("table:navigatorBottom:navigator:next", true);
        List<TagTester> images = TagTester.createTags(
                tester.getLastResponseAsString(), tag -> tag.getName().equalsIgnoreCase("img"), false);
        assertThat(images, not(empty()));
        images.stream()
                .map(image -> image.getAttribute("src"))
                .forEach(src -> assertThat(src, allOf(containsString("/img/icons/"), endsWith(".png"))));
    }
}
