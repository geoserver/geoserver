/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.junit.Assert.assertEquals;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.Collections;
import java.util.Map;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class KMLSuperOverlayTest extends WMSTestSupport {

    public static QName DISPERSED_FEATURES = new QName(MockData.SF_URI, "Dispersed",
            MockData.SF_PREFIX);
    private XpathEngine xpath;

    @Before
    public void setupXPath() {
        XMLUnit xpathXMLUnit;
        xpath = XMLUnit.newXpathEngine();
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("allsymbolizers", "allsymbolizers.sld", getClass(), catalog);
        testData.addStyle("SingleFeature", "singlefeature.sld", getClass(), catalog);
        testData.addStyle("Bridge", "bridge.sld", getClass(), catalog);
        testData.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/bridge.png");
        testData.addVectorLayer(DISPERSED_FEATURES, Collections.EMPTY_MAP, "Dispersed.properties",
                getClass(), catalog);
        
        // set a low regionation limit so that superoverlays actually have something to do
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        ft.getMetadata().put("kml.regionateFeatureLimit", 1);
        getCatalog().save(ft);
    }

    /**
     * Verify that the tiles are produced for a request that encompasses the world.
     */
    @Test
    public void testWorldBoundsSuperOverlay() throws Exception {
        Document document = getAsDOM("wms/kml?layers=" + getLayerId(MockData.BASIC_POLYGONS) + ","
                + getLayerId(DISPERSED_FEATURES) + "&mode=superoverlay");
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        // two folders, one per layer
        assertEquals("2", xpath.evaluate("count(//kml:Folder)", document));
        // regions: one whole world, two regions in links to the sub-tiles, two for the contents, per two layers
        assertEquals(9, document.getElementsByTagName("Region").getLength());
        // links: two layers, two sublinks and two contents per layer 
        assertEquals("8", xpath.evaluate("count(//kml:NetworkLink)", document));
        // no ground overlays, direct links to contents instead
        assertEquals("0", xpath.evaluate("count(//kml:GroundOverlay)", document));
        
        // overall bbox
        assertXpathEvaluatesTo("90.0", "//kml:Region/kml:LatLonAltBox/kml:north", document);
        assertXpathEvaluatesTo("-90.0", "//kml:Region/kml:LatLonAltBox/kml:south", document);
        assertXpathEvaluatesTo("180.0", "//kml:Region/kml:LatLonAltBox/kml:east", document);
        assertXpathEvaluatesTo("-180.0", "//kml:Region/kml:LatLonAltBox/kml:west", document);
    }
    
    /**
     * Check the link contents a bit
     */
    @Test
    public void testSuperOverlayLinkContents() throws Exception {
        
        Document document = getAsDOM("wms/kml?layers=" + getLayerId(MockData.BASIC_POLYGONS) + "&mode=superoverlay");
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        // two folders, one per layer
        assertEquals("1", xpath.evaluate("count(//kml:Folder)", document));
        // regions: one whole world, two regions in links to the sub-tiles, two for the contents
        assertEquals(5, document.getElementsByTagName("Region").getLength());
        // links: two sublinks and two contents per layer 
        assertEquals("4", xpath.evaluate("count(//kml:NetworkLink)", document));
        // no ground overlays, direct links to contents instead
        assertEquals("0", xpath.evaluate("count(//kml:GroundOverlay)", document));
        
        // check sub-link 0, it should still got to the network link builder
        String link0 = xpath.evaluate("//kml:NetworkLink[kml:name='0']/kml:Link/kml:href", document);
        Map<String, Object> kvp0 = KvpUtils.parseQueryString(link0);
        assertEquals(NetworkLinkMapOutputFormat.KML_MIME_TYPE, kvp0.get("format"));
        assertEquals("-180.0,-90.0,0.0,90.0", kvp0.get("bbox"));
        
        // check sub-link 1, the other side of the world
        String link1 = xpath.evaluate("//kml:NetworkLink[kml:name='1']/kml:Link/kml:href", document);
        Map<String, Object> kvp1 = KvpUtils.parseQueryString(link1);
        assertEquals(NetworkLinkMapOutputFormat.KML_MIME_TYPE, kvp1.get("format"));
        assertEquals("0.0,-90.0,180.0,90.0", kvp1.get("bbox"));
        
        // check the contents link 0
        String clink0 = xpath.evaluate("//kml:NetworkLink[kml:name='contents-0']/kml:Link/kml:href", document);
        Map<String, Object> kvpc0 = KvpUtils.parseQueryString(clink0);
        assertEquals(KMLMapOutputFormat.MIME_TYPE, kvpc0.get("format"));
        assertEquals("-180.0,-90.0,0.0,90.0", kvpc0.get("bbox"));

        // check the contents link 1
        String clink1 = xpath.evaluate("//kml:NetworkLink[kml:name='contents-1']/kml:Link/kml:href", document);
        Map<String, Object> kvpc1 = KvpUtils.parseQueryString(clink1);
        assertEquals(KMLMapOutputFormat.MIME_TYPE, kvpc1.get("format"));
        assertEquals("0.0,-90.0,180.0,90.0", kvpc1.get("bbox"));

        
    }

    /**
     * Verify that when a tile smaller than one hemisphere is requested, then subtiles are included
     * in the result (but only the ones necessary for the data at hand)
     */
    @Test
    public void testSubtileSuperOverlay() throws Exception {
        Document document = getAsDOM("wms/kml?layers=" + getLayerId(MockData.BASIC_POLYGONS) + ","
                + getLayerId(DISPERSED_FEATURES) + "&mode=superoverlay&bbox=0,-90,180,90");
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
    public void testKmlTitleFormatOption() throws Exception {
        Document document = getAsDOM("wms/kml?layers=" + getLayerId(MockData.BASIC_POLYGONS) + ","
                + getLayerId(DISPERSED_FEATURES) + "&mode=superoverlay&bbox=0,-90,180,90&format_options=kmltitle:myCustomLayerTitle");
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals("myCustomLayerTitle", xpath.evaluate("//kml:Document/kml:name", document));
    }
}
