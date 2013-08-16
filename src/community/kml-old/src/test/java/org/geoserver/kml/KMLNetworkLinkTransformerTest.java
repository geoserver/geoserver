/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static junit.framework.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSMockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

/**
 * Unit test suite for {@link KMLNetworkLinkTransformer}
 */
public class KMLNetworkLinkTransformerTest {

    private WMSMockData mockData;

    /**
     * The request to encode
     */
    private GetMapRequest request;

    private WMSMapContent mapContent;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @SuppressWarnings("rawtypes")
    @Before
    public void setUp() throws Exception {
        mockData = new WMSMockData();
        mockData.setUp();

        // Map<String, String> namespaces = new HashMap<String, String>();
        // namespaces.put("atom", "http://purl.org/atom/ns#");
        // XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        MapLayerInfo layer = mockData.addFeatureTypeLayer("TestPoints", Point.class);
        request = mockData.createRequest();
        request.setLayers(Collections.singletonList(layer));

        request.setFormatOptions(Collections.singletonMap("relLinks", "true"));
        request.setBaseUrl("http://geoserver.org:8181/geoserver");

        mapContent = new WMSMapContent(request);
        FeatureSource source = layer.getFeatureSource(true);
        Style layerStyle = mockData.getDefaultStyle().getStyle();
        Layer Layer = new FeatureLayer(source, layerStyle);
        mapContent.addLayer(Layer);
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @After
    public void tearDown() throws Exception {
        new GeoServerExtensions().setApplicationContext(null);
    }

    /**
     * Assert the encoding of the request as a Region inside the NetworkLink
     * 
     * @see KMLNetworkLinkTransformer#setEncodeAsRegion(boolean)
     */
    @Test
    public void testEncodeAsRegion() throws Exception {
        XpathEngine xpath = XMLUnit.newXpathEngine();

        WMS wms = mockData.getWMS();
        KMLNetworkLinkTransformer transformer = new KMLNetworkLinkTransformer(wms, mapContent);
        transformer.setEncodeAsRegion(true);
        transformer.setIndentation(2);

        request.setBbox(new Envelope(-1, 1, -10, 10));

        Document dom = WMSTestSupport.transform(mapContent, transformer);
        assertXpathEvaluatesTo("1", "count(//kml/Folder)", dom);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/NetworkLink)", dom);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/LookAt)", dom);

        assertXpathEvaluatesTo("geos:TestPoints", "//kml/Folder/NetworkLink/name", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/open", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/visibility", dom);
        // should match the request BBOX
        assertXpathEvaluatesTo("10.0", "//kml/Folder/NetworkLink/Region/LatLonAltBox/north", dom);
        assertXpathEvaluatesTo("-10.0", "//kml/Folder/NetworkLink/Region/LatLonAltBox/south", dom);
        assertXpathEvaluatesTo("1.0", "//kml/Folder/NetworkLink/Region/LatLonAltBox/east", dom);
        assertXpathEvaluatesTo("-1.0", "//kml/Folder/NetworkLink/Region/LatLonAltBox/west", dom);

        assertXpathEvaluatesTo("128", "//kml/Folder/NetworkLink/Region/Lod/minLodPixels", dom);
        assertXpathEvaluatesTo("-1", "//kml/Folder/NetworkLink/Region/Lod/maxLodPixels", dom);

        final Map<String, String> expectedKvp = KMLReflectorTest
                .toKvp("http://geoserver.org:8181/geoserver/wms?format_options=relLinks%3Atrue%3B&service=wms&srs=EPSG%3A4326&width=512&styles=Default+Style&height=256&transparent=false&bbox=-1.0%2C-10.0%2C1.0%2C10.0&request=GetMap&layers=geos%3ATestPoints&format=application/vnd.google-earth.kmz&version=1.1.1");
        final Map<String, String> actualKvp = KMLReflectorTest.toKvp(xpath.evaluate(
                "//kml/Folder/NetworkLink/Link/href", dom));
        KMLReflectorTest.assertMapsEqual(expectedKvp, actualKvp);

        // GR: commenting out the bellow assertion, adding the viewRefreshMode element is commented
        // out in KMLNetworkListTransformer
        // assertXpathEvaluatesTo("onRegion", "//kml/Folder/NetworkLink/Link/viewRefreshMode", dom);

        // feature type bounds?
        assertXpathEvaluatesTo("180.0", "//kml/Folder/LookAt/longitude", dom);
        assertXpathEvaluatesTo("0.0", "//kml/Folder/LookAt/latitude", dom);
        assertXpathExists("//kml/Folder/LookAt/altitude", dom);

        assertXpathEvaluatesTo("0.0", "//kml/Folder/LookAt/tilt", dom);
        assertXpathEvaluatesTo("0.0", "//kml/Folder/LookAt/heading", dom);
        assertXpathEvaluatesTo("clampToGround", "//kml/Folder/LookAt/altitudeMode", dom);
        // these ones are hard to compute, maybe someone understanding what's going on could add an
        // expected value?
        String value = xpath.evaluate("//kml/Folder/LookAt/altitude", dom);
        assertEquals(1.5768778343115222E7, Double.parseDouble(value), 1E-5);
        assertXpathExists("//kml/Folder/LookAt/range", dom);
    }

    /**
     * If {@code KMLNetworkLinkTransformer.setEncodeAsRegion(false)} (default behaviour), the
     * request is encoded as an overlay instead than as a Region
     * 
     * @throws Exception
     */
    @Test
    public void testEncodeAsOverlay() throws Exception {
        XpathEngine xpath = XMLUnit.newXpathEngine();
        WMS wms = mockData.getWMS();
        KMLNetworkLinkTransformer transformer = new KMLNetworkLinkTransformer(wms, mapContent);
        transformer.setEncodeAsRegion(false);
        transformer.setIndentation(2);

        request.setBbox(new Envelope(-1, 1, -10, 10));

        Document dom = WMSTestSupport.transform(mapContent, transformer);
        assertXpathEvaluatesTo("1", "count(//kml/Folder)", dom);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/NetworkLink)", dom);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/LookAt)", dom);

        assertXpathEvaluatesTo("geos:TestPoints", "//kml/Folder/NetworkLink/name", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/open", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/visibility", dom);

        final Map<String, String> expectedKvp = KMLReflectorTest
                .toKvp("http://geoserver.org:8181/geoserver/wms?format_options=relLinks%3Atrue%3B&service=wms&srs=EPSG%3A4326&width=512&styles=Default+Style&height=256&transparent=false&request=GetMap&layers=geos%3ATestPoints&format=application/vnd.google-earth.kmz&version=1.1.1");
        final Map<String, String> actualKvp = KMLReflectorTest.toKvp(xpath.evaluate(
                "//kml/Folder/NetworkLink/Url/href", dom));
        KMLReflectorTest.assertMapsEqual(expectedKvp, actualKvp);
        assertXpathEvaluatesTo("onStop", "//kml/Folder/NetworkLink/Url/viewRefreshMode", dom);
        assertXpathEvaluatesTo("1", "//kml/Folder/NetworkLink/Url/viewRefreshTime", dom);

        // feature type bounds?
        assertXpathEvaluatesTo("180.0", "//kml/Folder/LookAt/longitude", dom);
        assertXpathEvaluatesTo("0.0", "//kml/Folder/LookAt/latitude", dom);
        assertXpathExists("//kml/Folder/LookAt/altitude", dom);
        assertXpathEvaluatesTo("0.0", "//kml/Folder/LookAt/tilt", dom);
        assertXpathEvaluatesTo("0.0", "//kml/Folder/LookAt/heading", dom);
        assertXpathEvaluatesTo("clampToGround", "//kml/Folder/LookAt/altitudeMode", dom);
        // these ones are hard to compute, maybe someone understanding what's going on could add an
        // expected value?
        String value = xpath.evaluate("//kml/Folder/LookAt/altitude", dom);
        assertEquals(1.5768778343115222E7, Double.parseDouble(value), 1E-5);
        assertXpathExists("//kml/Folder/LookAt/range", dom);
    }

    /**
     * @see KMLLookAt
     */
    @Test
    public void testEncodeLookAtVendorSpecificParameters() throws Exception {
        WMS wms = mockData.getWMS();
        KMLNetworkLinkTransformer transformer = new KMLNetworkLinkTransformer(wms, mapContent);
        transformer.setEncodeAsRegion(false);
        transformer.setIndentation(2);

        request.setBbox(new Envelope(-1, 1, -10, 10));

        Map<String, Object> formatOptions = new HashMap<String, Object>();
        formatOptions.put("LOOKATGEOM", "POINT(1 1)");
        formatOptions.put("ALTITUDE", "500");
        formatOptions.put("HEADING", "45");
        formatOptions.put("TILT", "30");
        formatOptions.put("RANGE", "2000");
        formatOptions.put("ALTITUDEMODE", "absolute");

        request.setFormatOptions(formatOptions);

        Document dom = WMSTestSupport.transform(mapContent, transformer);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/LookAt)", dom);

        // explicit lookAt properties as set by FORMAT_OPTIONS
        assertXpathEvaluatesTo("1.0", "//kml/Folder/LookAt/longitude", dom);
        assertXpathEvaluatesTo("1.0", "//kml/Folder/LookAt/latitude", dom);
        assertXpathEvaluatesTo("500.0", "//kml/Folder/LookAt/altitude", dom);
        assertXpathEvaluatesTo("45.0", "//kml/Folder/LookAt/heading", dom);
        assertXpathEvaluatesTo("30.0", "//kml/Folder/LookAt/tilt", dom);
        assertXpathEvaluatesTo("2000.0", "//kml/Folder/LookAt/range", dom);
        assertXpathEvaluatesTo("absolute", "//kml/Folder/LookAt/altitudeMode", dom);
    }
    
    @Test
    public void testKmltitleFormatOption() throws Exception {
        WMS wms = mockData.getWMS();
        KMLNetworkLinkTransformer transformer = new KMLNetworkLinkTransformer(wms, mapContent);
        transformer.setEncodeAsRegion(false);
        transformer.setIndentation(2);

        request.setBbox(new Envelope(-1, 1, -10, 10));

        Map<String, Object> formatOptions = new HashMap<String, Object>();
        formatOptions.put("kmltitle", "myCustomLayerTitle");
        request.setFormatOptions(formatOptions);

        Document dom = WMSTestSupport.transform(mapContent, transformer);
        assertXpathEvaluatesTo("1", "count(//kml/Folder/name)", dom);

        // explicit lookAt properties as set by FORMAT_OPTIONS
        assertXpathEvaluatesTo("myCustomLayerTitle", "//kml/Folder/name", dom);
    } 
}
