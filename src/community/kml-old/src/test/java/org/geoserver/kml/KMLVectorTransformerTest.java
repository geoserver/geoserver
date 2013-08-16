/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static junit.framework.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSMockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollections;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;

import com.vividsolutions.jts.geom.Point;

/**
 * Unit test suite for {@link KMLVectorTransformer}
 */
public class KMLVectorTransformerTest  {

    private WMSMockData mockData;
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        mockData = new WMSMockData();
        mockData.setUp();

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("atom", "http://purl.org/atom/ns#");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @After
    public void tearDown() throws Exception {
        new GeoServerExtensions().setApplicationContext(null);
    }
    
    /**
     * If {@link KMLVectorTransformer#isStandAlone()} then the root element is Document, otherwise
     * its kml
     * 
     * @throws Exception
     */
    @Test
    public void testSetStandAlone() throws Exception {
        SimpleFeatureCollection features = FeatureCollections
                .newCollection();
        Style style = mockData.getDefaultStyle().getStyle();
        Layer layer = new FeatureLayer(features, style);

        WMSMapContent mapContent = new WMSMapContent();
        GetMapRequest request = mockData.createRequest();
        mapContent.setRequest(request);

        KMLVectorTransformer transformer = new KMLVectorTransformer(mockData.getWMS(), mapContent, layer);

        Document document;

        transformer.setStandAlone(true);
        document = WMSTestSupport.transform(features, transformer);
        assertEquals("kml", document.getDocumentElement().getNodeName());

        transformer.setStandAlone(false);
        document = WMSTestSupport.transform(features, transformer);
        assertEquals("Document", document.getDocumentElement().getNodeName());
    }

    /**
     * Paging is only enabled if the request has the {@code maxfeatures} parameter set and the
     * {@code relLinks} parameter set to {@code true}.
     * 
     * @throws IOException
     * @see GetMapRequest#getMaxFeatures()
     * @see GetMapRequest#getStartIndex()
     */
    @Test
    public void testEncodeWithPaging() throws Exception {
        MapLayerInfo mli = mockData.addFeatureTypeLayer("TestPoints", Point.class);
        FeatureTypeInfo typeInfo = mli.getFeature();
        SimpleFeatureType featureType = (SimpleFeatureType) typeInfo.getFeatureType();
        mockData.addFeature(featureType, new Object[] { "name1", "POINT(1 1)" });
        mockData.addFeature(featureType, new Object[] { "name2", "POINT(2 2)" });
        mockData.addFeature(featureType, new Object[] { "name3", "POINT(3 3)" });
        mockData.addFeature(featureType, new Object[] { "name4", "POINT(4 4)" });

        SimpleFeatureSource fs = 
            (SimpleFeatureSource) typeInfo.getFeatureSource(null, null);
        SimpleFeatureCollection features = fs.getFeatures();

        Style style = mockData.getDefaultStyle().getStyle();
        Layer layer = new FeatureLayer(features, style);
        layer.setTitle("TestPointsTitle");

        GetMapRequest request = mockData.createRequest();
        request.setLayers(Collections.singletonList(mli));

        request.setMaxFeatures(2);
        request.setStartIndex(2);
        request.setFormatOptions(Collections.singletonMap("relLinks", "true"));
        request.setBaseUrl("baseurl");
        WMSMapContent mapContent = new WMSMapContent();
        mapContent.setRequest(request);

        KMLVectorTransformer transformer = new KMLVectorTransformer(mockData.getWMS(), mapContent, layer);
        transformer.setStandAlone(false);
        transformer.setIndentation(2);

        Document dom = WMSTestSupport.transform(features, transformer);
        assertXpathExists("//Document/name", dom);
        assertXpathEvaluatesTo("TestPointsTitle", "//Document/name", dom);
        assertXpathExists("//Document/atom:link", dom);
        assertXpathEvaluatesTo("prev", "//Document/atom:link[1]/@rel", dom);
        assertXpathEvaluatesTo("next", "//Document/atom:link[2]/@rel", dom);

        // we're at startIndex=2 and maxFeatures=2, so expect previous link to be 0, and next to be
        // 4
        String expectedLink;
        expectedLink = "baseurl/rest/geos/TestPoints.kml?startindex=0&maxfeatures=2";
        assertXpathEvaluatesTo(expectedLink, "//Document/atom:link[1]/@href", dom);
        expectedLink = "baseurl/rest/geos/TestPoints.kml?startindex=4&maxfeatures=2";
        assertXpathEvaluatesTo(expectedLink, "//Document/atom:link[2]/@href", dom);

        assertXpathEvaluatesTo("prev", "//Document/NetworkLink[1]/@id", dom);
        assertXpathEvaluatesTo("next", "//Document/NetworkLink[2]/@id", dom);

        expectedLink = "baseurl/rest/geos/TestPoints.kml?startindex=0&maxfeatures=2";
        assertXpathEvaluatesTo(expectedLink, "//Document/NetworkLink[1]/Link/href", dom);

        expectedLink = "baseurl/rest/geos/TestPoints.kml?startindex=4&maxfeatures=2";
        assertXpathEvaluatesTo("next", "//Document/NetworkLink[2]/@id", dom);
    }
    
    @Test
    public void testKmltitleFormatOption() throws Exception {
        SimpleFeatureCollection features = FeatureCollections
                .newCollection();
        Style style = mockData.getDefaultStyle().getStyle();
        Layer layer = new FeatureLayer(features, style);

        WMSMapContent mapContent = new WMSMapContent();
        GetMapRequest request = mockData.createRequest();
        mapContent.setRequest(request);

        Map<String, Object> formatOptions = new HashMap<String, Object>();
        formatOptions.put("kmltitle", "myCustomLayerTitle");
        mapContent.getRequest().setFormatOptions(formatOptions);

        KMLVectorTransformer transformer = new KMLVectorTransformer(mockData.getWMS(), mapContent, layer);

        Document document;

        transformer.setStandAlone(true);
        document = WMSTestSupport.transform(features, transformer);
        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals("Document", document.getDocumentElement().getFirstChild().getNodeName());
        assertEquals("myCustomLayerTitle", document.getDocumentElement().getFirstChild().getFirstChild().getTextContent());
    }
    
}
