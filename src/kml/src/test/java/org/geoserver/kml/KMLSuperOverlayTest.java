/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class KMLSuperOverlayTest extends WMSTestSupport {

    public static QName DISPERSED_FEATURES =
            new QName(MockData.SF_URI, "Dispersed", MockData.SF_PREFIX);
    public static QName BOULDER = new QName(MockData.SF_URI, "boulder", MockData.SF_PREFIX);
    private XpathEngine xpath;

    @Before
    public void setupXPath() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs10RasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("allsymbolizers", "allsymbolizers.sld", getClass(), catalog);
        testData.addStyle("SingleFeature", "singlefeature.sld", getClass(), catalog);
        testData.addStyle("Bridge", "bridge.sld", getClass(), catalog);
        testData.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/bridge.png");
        testData.addVectorLayer(
                DISPERSED_FEATURES,
                Collections.EMPTY_MAP,
                "Dispersed.properties",
                getClass(),
                catalog);
        Map<SystemTestData.LayerProperty, Object> properties =
                new HashMap<SystemTestData.LayerProperty, Object>();
        properties.put(
                LayerProperty.LATLON_ENVELOPE,
                new ReferencedEnvelope(-105.336, -105.112, 39.9, 40.116, CRS.decode("EPSG:4326")));
        properties.put(
                LayerProperty.ENVELOPE,
                new ReferencedEnvelope(
                        3045967, 3108482, 1206627, 1285209, CRS.decode("EPSG:2876")));
        properties.put(LayerProperty.SRS, 2876);
        testData.addVectorLayer(BOULDER, properties, "boulder.properties", getClass(), catalog);

        // set a low regionation limit so that superoverlays actually have something to do
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        ft.getMetadata().put("kml.regionateFeatureLimit", 1);
        getCatalog().save(ft);
    }

    /** Verify that the tiles are produced for a request that encompasses the world. */
    @Test
    public void testWorldBoundsSuperOverlay() throws Exception {
        Document document =
                getAsDOM(
                        "wms/kml?layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + ","
                                + getLayerId(DISPERSED_FEATURES)
                                + "&mode=superoverlay");
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        // two folders, one per layer
        assertEquals("2", xpath.evaluate("count(//kml:Folder)", document));
        // regions
        assertEquals(9, document.getElementsByTagName("Region").getLength());
        // links
        assertEquals("8", xpath.evaluate("count(//kml:NetworkLink)", document));
        // no ground overlays, direct links to contents instead
        assertEquals("0", xpath.evaluate("count(//kml:GroundOverlay)", document));

        // overall bbox
        assertXpathEvaluatesTo("90.0", "//kml:Region/kml:LatLonAltBox/kml:north", document);
        assertXpathEvaluatesTo("-90.0", "//kml:Region/kml:LatLonAltBox/kml:south", document);
        assertXpathEvaluatesTo("180.0", "//kml:Region/kml:LatLonAltBox/kml:east", document);
        assertXpathEvaluatesTo("-180.0", "//kml:Region/kml:LatLonAltBox/kml:west", document);

        // check we have contents starting from the top
        assertXpathExists("//kml:NetworkLink[kml:name='contents-0']", document);
        assertXpathExists("//kml:NetworkLink[kml:name='contents-1']", document);
    }

    /**
     * Checks what happens when the data bbox is at the crossing of a parent tile that is two levels
     * above the bbox itself
     */
    @Test
    public void testCrossingSuperoverlay() throws Exception {
        Document document =
                getAsDOM("wms/kml?layers=" + getLayerId(BOULDER) + "&mode=superoverlay");
        // print(document);

        // check the overall bbox (the top-most tile that contains all data)
        assertXpathEvaluatesTo("40.78125", "//kml:Region/kml:LatLonAltBox/kml:north", document);
        assertXpathEvaluatesTo("39.375", "//kml:Region/kml:LatLonAltBox/kml:south", document);
        assertXpathEvaluatesTo("-104.0625", "//kml:Region/kml:LatLonAltBox/kml:east", document);
        assertXpathEvaluatesTo("-105.46875", "//kml:Region/kml:LatLonAltBox/kml:west", document);

        // however the lookats are pointing to the center of the data set
        assertXpathEvaluatesTo(
                "-105.22419118401743", "//kml:Document/kml:LookAt/kml:longitude", document);
        assertXpathEvaluatesTo(
                "40.008056082289826", "//kml:Document/kml:LookAt/kml:latitude", document);

        assertEquals(
                -105.2243,
                Double.parseDouble(
                        xpath.evaluate(
                                "//kml:Document/kml:Folder/kml:LookAt/kml:longitude", document)),
                1E-4);
        assertEquals(
                40.0081,
                Double.parseDouble(
                        xpath.evaluate(
                                "//kml:Document/kml:Folder/kml:LookAt/kml:latitude", document)),
                1E-4);
    }

    /** Check the link contents a bit */
    @Test
    public void testSuperOverlayLinkContents() throws Exception {

        Document document =
                getAsDOM(
                        "wms/kml?layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&mode=superoverlay");
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        // two folders, one per layer
        assertEquals("1", xpath.evaluate("count(//kml:Folder)", document));
        // regions
        assertEquals(5, document.getElementsByTagName("Region").getLength());
        // links
        assertEquals("4", xpath.evaluate("count(//kml:NetworkLink)", document));
        // no ground overlays, direct links to contents instead
        assertEquals("0", xpath.evaluate("count(//kml:GroundOverlay)", document));

        // check sub-link 0, it should still got to the network link builder
        String link0 =
                xpath.evaluate("//kml:NetworkLink[kml:name='0']/kml:Link/kml:href", document);
        Map<String, Object> kvp0 = KvpUtils.parseQueryString(link0);
        assertEquals(NetworkLinkMapOutputFormat.KML_MIME_TYPE, kvp0.get("format"));
        assertEquals("-180.0,-90.0,0.0,90.0", kvp0.get("bbox"));

        // check sub-link 1, the other side of the world
        String link1 =
                xpath.evaluate("//kml:NetworkLink[kml:name='1']/kml:Link/kml:href", document);
        Map<String, Object> kvp1 = KvpUtils.parseQueryString(link1);
        assertEquals(NetworkLinkMapOutputFormat.KML_MIME_TYPE, kvp1.get("format"));
        assertEquals("0.0,-90.0,180.0,90.0", kvp1.get("bbox"));
    }

    /**
     * Verify that when a tile smaller than one hemisphere is requested, then subtiles are included
     * in the result (but only the ones necessary for the data at hand)
     */
    @Test
    public void testSubtileSuperOverlay() throws Exception {
        Document document =
                getAsDOM(
                        "wms/kml?layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + ","
                                + getLayerId(DISPERSED_FEATURES)
                                + "&mode=superoverlay&bbox=0,-90,180,90");
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        // only three regions, the root one and one per network link
        assertEquals(3, document.getElementsByTagName("Region").getLength());
        // only network links to the data, we don't have enough feature to have sublinks generate
        assertEquals(2, document.getElementsByTagName("NetworkLink").getLength());
        // no ground overlays
        assertEquals(0, document.getElementsByTagName("GroundOverlay").getLength());
    }

    @Test
    public void testGWCIntegration() throws Exception {
        Document document =
                getAsDOM(
                        "wms/kml?layers="
                                + getLayerId(MockData.USA_WORLDIMG)
                                + "&mode=superoverlay&superoverlay_mode=cached");
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        // only three regions, the root one and one per network link
        assertEquals(1, document.getElementsByTagName("Region").getLength());
        // only network links to the data, we don't have enough feature to have sublinks generate
        assertEquals(1, document.getElementsByTagName("NetworkLink").getLength());
        // no ground overlays
        assertEquals(0, document.getElementsByTagName("GroundOverlay").getLength());

        // check we have a direct link to GWC
        assertEquals(
                "http://localhost:8080/geoserver/gwc/service/kml/cdf:usa.png.kml",
                xpath.evaluate("//kml:NetworkLink/kml:Link/kml:href", document));
        assertEquals(
                "never",
                xpath.evaluate("//kml:NetworkLink/kml:Link/kml:viewRefreshMode", document));
    }

    @Test
    public void testGWCIntegrationFailing() throws Exception {
        // force placemarks, this prevents usage of gwc
        Document document =
                getAsDOM(
                        "wms/kml?layers="
                                + getLayerId(MockData.USA_WORLDIMG)
                                + "&mode=superoverlay&superoverlay_mode=cached&kmplacemark=true");
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(6, document.getElementsByTagName("Region").getLength());
        assertEquals(4, document.getElementsByTagName("NetworkLink").getLength());
        // no ground overlays
        assertEquals(1, document.getElementsByTagName("GroundOverlay").getLength());

        // check we do not have a direct link to GWC, but back to the wms
        assertTrue(
                "http://localhost:8080/geoserver/gwc/service/kml/cdf:usa.png.kml",
                xpath.evaluate("//kml:NetworkLink/kml:Link/kml:href", document)
                        .contains("geoserver/wms"));
    }

    @Test
    public void testKmlTitleFormatOption() throws Exception {
        Document document =
                getAsDOM(
                        "wms/kml?layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + ","
                                + getLayerId(DISPERSED_FEATURES)
                                + "&mode=superoverlay&bbox=0,-90,180,90&format_options=kmltitle:myCustomLayerTitle");
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals("myCustomLayerTitle", xpath.evaluate("//kml:Document/kml:name", document));
    }
}
