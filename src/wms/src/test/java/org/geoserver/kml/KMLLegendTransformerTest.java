/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WMSMockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollections;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapLayer;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpSession;
import com.mockrunner.mock.web.MockServletContext;
import com.vividsolutions.jts.geom.Point;

/**
 * Unit test suite for {@link KMLLegendTransformer}
 * 
 * @author Gabriel Roldan (OpenGeo)
 * @version $Id$
 */
public class KMLLegendTransformerTest extends TestCase {

    private WMSMockData mockData;

    /**
     * The map context for the transformer constructor. It shouldn't be needed, see the comment at
     * {@link KMLLegendTransformer#KMLLegendTransformer(WMSMapContext)}
     */
    private WMSMapContext mapContext;

    /**
     * The layer to encode the legend url for
     */
    private MapLayer mapLayer;
    
    private MockHttpServletRequest httpreq;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        mockData = new WMSMockData();
        mockData.setUp();

        httpreq = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        httpreq.setSession(session);
        MockServletContext context = new MockServletContext();
        session.setupServletContext(context);
        
        // Map<String, String> namespaces = new HashMap<String, String>();
        // namespaces.put("atom", "http://purl.org/atom/ns#");
        // XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        MapLayerInfo layer = mockData.addFeatureTypeLayer("TestPoints", Point.class);
        mapContext = new WMSMapContext();
        GetMapRequest request = mockData.createRequest();
        request.setLayers(Collections.singletonList(layer));

        SimpleFeatureSource featureSource;
        featureSource = (SimpleFeatureSource) ((FeatureTypeInfo)layer.getFeature()).getFeatureSource(null, null);
        
        mapLayer = new DefaultMapLayer(featureSource, mockData.getDefaultStyle().getStyle());

        httpreq.setScheme("http");
        httpreq.setServerName("geoserver.org");
        httpreq.setServerPort(8181);
        httpreq.setContextPath("/geoserver");
        mapContext.setRequest(request);
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link KMLLegendTransformer#KMLLegendTransformer(org.geoserver.wms.WMSMapContext)}.
     * 
     * @throws Exception
     */
    public void testKMLLegendTransformer() throws Exception {
        SimpleFeatureCollection features = FeatureCollections
                .newCollection();
        XpathEngine xpath = XMLUnit.newXpathEngine();

        KMLLegendTransformer transformer = new KMLLegendTransformer(mapContext);
        transformer.setIndentation(2);
        Document dom = WMSTestSupport.transform(mapLayer, transformer);
        assertXpathEvaluatesTo("Legend", "//kml/ScreenOverlay/name", dom);
        assertXpathEvaluatesTo("0", "//kml/ScreenOverlay/overlayXY/@x", dom);
        assertXpathEvaluatesTo("0", "//kml/ScreenOverlay/overlayXY/@y", dom);
        assertXpathEvaluatesTo("pixels", "//kml/ScreenOverlay/overlayXY/@xunits", dom);
        assertXpathEvaluatesTo("pixels", "//kml/ScreenOverlay/overlayXY/@yunits", dom);

        Map<String, String> expectedKVP = KMLReflectorTest
                .toKvp("http://geoserver.org:8181/geoserver/wms?service=wms&width=20&height=20&style=Default+Style&request=GetLegendGraphic&layer=&format=image%2Fpng&version=1.1.1");
        Map<String, String> resultantKVP = KMLReflectorTest.toKvp(xpath.evaluate(
                "//kml/ScreenOverlay/Icon/href", dom));

        KMLReflectorTest.assertMapsEqual(expectedKVP, resultantKVP);
    }
}
