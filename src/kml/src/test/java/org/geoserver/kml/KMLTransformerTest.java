/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.kml.KMZMapResponse.KMZMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class KMLTransformerTest extends WMSTestSupport {
    WMSMapContent mapContent;

    Layer layer;

  
    @Before
    public void setMapContent() throws Exception {
        
        layer = createMapLayer(MockData.BASIC_POLYGONS);

        mapContent = new WMSMapContent(createGetMapRequest(MockData.BASIC_POLYGONS));
        mapContent.addLayer(layer);
    }

    @After
    public void unsetMapContent() {
        mapContent.dispose();
        // mapContent.clearLayerList();
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog =getCatalog();
        testData.addStyle("allsymbolizers","allsymbolizers.sld",getClass(), catalog);
        testData.addStyle("SingleFeature","singlefeature.sld",getClass(), catalog);
        testData.addStyle("Bridge","bridge.sld",getClass(), catalog);
        testData.addStyle("BridgeSubdir","bridgesubdir.sld",getClass(), catalog);
        testData.addStyle("dynamicsymbolizer","dynamicsymbolizer.sld",getClass(), catalog);
        testData.addStyle("relativeds","relativeds.sld",getClass(), catalog);
        testData.addStyle("big-local-image","/big-local-image.sld",getClass(), catalog);
        testData.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/bridge.png");
        testData.copyTo(getClass().getResourceAsStream("/planet-42.png"), "styles/planet-42.png");
        File stylesDir = new File(testData.getDataDirectoryRoot(),"styles");
        new File(stylesDir,"graphics").mkdir();
        testData.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/graphics/bridgesubdir.png");
    }

    @Test
    public void testVectorTransformer() throws Exception {
        KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContent, layer);
        transformer.setIndentation(2);

        SimpleFeatureSource featureSource = DataUtilities.simple((FeatureSource) layer
                .getFeatureSource());
        int nfeatures = featureSource.getFeatures().size();

        Document document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);

        Element element = document.getDocumentElement();
        assertEquals("kml", element.getNodeName());
        assertEquals(nfeatures, element.getElementsByTagName("Style").getLength());
        assertEquals(nfeatures, element.getElementsByTagName("Placemark").getLength());
    }

    /**
     * See http://jira.codehaus.org/browse/GEOS-1947
     * 
     * @throws Exception
     */
    @Test
    public void testExternalGraphicBackround() throws Exception {

        Layer Layer = createMapLayer(MockData.POINTS, "Bridge");
        Document document;

        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (SimpleFeatureSource) Layer.getFeatureSource();
        int nfeatures = featureSource.getFeatures().size();
        WMSMapContent mapContent = new WMSMapContent(createGetMapRequest(MockData.POINTS));
        try {
            mapContent.addLayer(Layer);
            KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContent,
                    Layer);
            transformer.setIndentation(2);

            // print(document);
            document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);
        } finally {
            mapContent.dispose();
        }
        // make sure we are generating icon styles, but that we're not sticking a color onto them
        XMLAssert.assertXpathEvaluatesTo("" + nfeatures, "count(//Style/IconStyle/Icon/href)",
                document);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//Style/IconStyle/Icon/color)", document);

    }

    /**
     * See http://jira.codehaus.org/browse/GEOS-3994
     * 
     * @throws Exception
     */
    @Test
    public void testExternalGraphicSubdir() throws Exception {

        Layer layer = createMapLayer(MockData.POINTS, "BridgeSubdir");

        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (SimpleFeatureSource) layer.getFeatureSource();

        WMSMapContent mapContent = new WMSMapContent(createGetMapRequest(MockData.POINTS));
        Document document;
        try {
            mapContent.addLayer(layer);
            KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContent,
                    layer);
            transformer.setIndentation(2);
            // print(document);
            document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);
        } finally {
            mapContent.dispose();
        }
        // make sure we are generating icon styles with the subdir path
        XMLAssert.assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/styles/graphics/bridgesubdir.png",
                "//Style/IconStyle/Icon/href", document);
    }

    /**
     * See http://jira.codehaus.org/browse/GEOS-3965
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testProxyBaseURL() throws Exception {
        GeoServer gs = getGeoServer();
        try {
            GeoServerInfo info = gs.getGlobal();
            info.setProxyBaseUrl("http://myhost:9999/gs");
            gs.save(info);

            Layer layer = createMapLayer(MockData.POINTS, "Bridge");
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
            featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) layer
                    .getFeatureSource();
            int nfeatures = featureSource.getFeatures().size();

            WMSMapContent mapContent = new WMSMapContent(createGetMapRequest(MockData.POINTS));
            Document document;

            try {
                mapContent.addLayer(layer);
                KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContent,
                        layer);
                transformer.setIndentation(2);

                document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);
                // print(document);
            } finally {
                mapContent.dispose();
            }
            // make sure we are using the proxy base URL
            XMLAssert.assertXpathEvaluatesTo("http://myhost:9999/gs/styles/bridge.png",
                    "//Style/IconStyle/Icon/href", document);
        } finally {
            GeoServerInfo info = gs.getGlobal();
            info.setProxyBaseUrl(null);
            gs.save(info);
        }
    }

    @Test
    public void testFilteredData() throws Exception {
        Layer layer = createMapLayer(MockData.BASIC_POLYGONS, "SingleFeature");

        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (SimpleFeatureSource) layer.getFeatureSource();

        WMSMapContent mapContent = new WMSMapContent(createGetMapRequest(MockData.BASIC_POLYGONS));
        Document document;
        try {
            mapContent.addLayer(layer);

            KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContent,
                    layer);
            transformer.setIndentation(2);
            document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);
        } finally {
            mapContent.dispose();
        }
        Element element = document.getDocumentElement();
        assertEquals("kml", element.getNodeName());
        assertEquals(1, element.getElementsByTagName("Placemark").getLength());
        assertEquals(1, element.getElementsByTagName("Style").getLength());
    }

    // @Test
    //public void testReprojection() throws Exception {
    // KMLTransformer transformer = new KMLTransformer();
    // transformer.setIndentation(2);
    //
    // ByteArrayOutputStream output = new ByteArrayOutputStream();
    // transformer.transform(mapContent, output);
    // transformer.transform(mapContent,System.out);
    // DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    // Document doc1 = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
    //
    // mapContent.setCoordinateReferenceSystem(CRS.decode("EPSG:3005"));
    // output = new ByteArrayOutputStream();
    // transformer.transform(mapContent, output);
    // transformer.transform(mapContent,System.out);
    // Document doc2 = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
    //
    // NodeList docs1 = doc1.getDocumentElement().getElementsByTagName("Document");
    // NodeList docs2 = doc2.getDocumentElement().getElementsByTagName("Document");
    //
    // assertEquals( docs1.getLength(), docs2.getLength() );
    // for ( int i = 0; i < docs1.getLength(); i++ ) {
    // Element e1 = (Element) docs1.item(i);
    // Element e2 = (Element) docs2.item(i);
    //
    // String name1 = ReaderUtils.getChildText( e1, "name" );
    // String name2 = ReaderUtils.getChildText( e2, "name" );
    //
    // assertEquals( name1, name2 );
    //
    // Element p1 = (Element) e1.getElementsByTagName("Placemark").item(0);
    // Element p2 = (Element) e2.getElementsByTagName("Placemark").item(0);
    //
    // Element poly1 = (Element) p1.getElementsByTagName("Polygon").item(0);
    // Element poly2 = (Element) p2.getElementsByTagName("Polygon").item(0);
    //
    // Element c1 = (Element) poly1.getElementsByTagName("coordinates").item(0);
    // Element c2 = (Element) poly2.getElementsByTagName("coordinates").item(0);
    //
    // assertFalse(c1.getFirstChild().getNodeValue().equals( c2.getFirstChild().getNodeValue()));
    // }
    //
    // }

    @Test
    public void testRasterTransformerInline() throws Exception {
        KMLRasterTransformer transformer = new KMLRasterTransformer(getWMS(), mapContent);
        transformer.setInline(true);

        Document document = WMSTestSupport.transform(layer, transformer);

        assertEquals("kml", document.getDocumentElement().getNodeName());

        assertEquals(mapContent.layers().size(), document.getElementsByTagName("Folder")
                .getLength());
        assertEquals(mapContent.layers().size(), document.getElementsByTagName("GroundOverlay")
                .getLength());

        assertEquals(mapContent.layers().size(), document.getElementsByTagName("href").getLength());

        Element href = (Element) document.getElementsByTagName("href").item(0);
        assertEquals("images/layer_0.png", href.getFirstChild().getNodeValue());
    }

    @Test
    public void testRasterTransformerNotInline() throws Exception {
        KMLRasterTransformer transformer = new KMLRasterTransformer(getWMS(), mapContent);
        transformer.setInline(false);

        Document document = WMSTestSupport.transform(layer, transformer);

        assertEquals("kml", document.getDocumentElement().getNodeName());

        assertEquals(mapContent.layers().size(), document.getElementsByTagName("Folder")
                .getLength());
        assertEquals(mapContent.layers().size(), document.getElementsByTagName("GroundOverlay")
                .getLength());

        assertEquals(mapContent.layers().size(), document.getElementsByTagName("href").getLength());

        Element href = (Element) document.getElementsByTagName("href").item(0);
        assertTrue(href.getFirstChild().getNodeValue().startsWith("http://localhost"));
    }

    @Test
    public void testRasterTransformerSLD() throws Exception {
        KMLRasterTransformer transformer = new KMLRasterTransformer(getWMS(), mapContent);

        mapContent.getRequest().setSld(new URL("http://my.external/dynamic/sldService"));
        Document document = WMSTestSupport.transform(layer, transformer);

        assertEquals(mapContent.layers().size(), document.getElementsByTagName("href").getLength());

        Element href = (Element) document.getElementsByTagName("href").item(0);

        assertTrue(href.getFirstChild().getNodeValue()
                .contains("&sld=http%3A%2F%2Fmy.external%2Fdynamic%2FsldService"));
    }
    
    @Test
    public void testRasterPlacemarkTrue() throws Exception {
        doTestRasterPlacemark(true);
    }

    @Test
    public void testRasterPlacemarkFalse() throws Exception {
        doTestRasterPlacemark(false);
    }

    protected void doTestRasterPlacemark(boolean doPlacemarks) throws Exception {
        GetMapRequest getMapRequest = createGetMapRequest(MockData.BASIC_POLYGONS);
        HashMap formatOptions = new HashMap();
        formatOptions.put("kmplacemark", new Boolean(doPlacemarks));
        formatOptions.put("kmscore", new Integer(0));
        getMapRequest.setFormatOptions(formatOptions);

        WMSMapContent mapContent = new WMSMapContent(getMapRequest);
        mapContent.addLayer(layer);
        mapContent.setMapHeight(1024);
        mapContent.setMapWidth(1024);

        // create the map producer
        KMZMapOutputFormat mapProducer = new KMZMapOutputFormat(getWMS());
        KMZMapResponse mapEncoder = new KMZMapResponse(getWMS());
        KMZMap kmzMap = mapProducer.produceMap(mapContent);
        try {
            // create the kmz
            File tempDir = IOUtils.createRandomDirectory("./target", "kmplacemark", "test");
            tempDir.deleteOnExit();

            File zip = new File(tempDir, "kmz.zip");
            zip.deleteOnExit();

            FileOutputStream output = new FileOutputStream(zip);
            mapEncoder.write(kmzMap, output, null);

            output.flush();
            output.close();

            assertTrue(zip.exists());

            // unzip and test it
            ZipFile zipFile = new ZipFile(zip);

            ZipEntry entry = zipFile.getEntry("wms.kml");
            assertNotNull(entry);
            assertNotNull(zipFile.getEntry("images/layer_0.png"));

            // unzip the wms.kml to file
            byte[] buffer = new byte[1024];
            int len;

            InputStream inStream = zipFile.getInputStream(entry);
            File temp = File.createTempFile("test_out", "kmz", tempDir);
            temp.deleteOnExit();
            BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(temp));

            while ((len = inStream.read(buffer)) >= 0)
                outStream.write(buffer, 0, len);
            inStream.close();
            outStream.close();

            // read in the wms.kml and check its contents
            Document document = dom(new BufferedInputStream(new FileInputStream(temp)));

            assertEquals("kml", document.getDocumentElement().getNodeName());
            if (doPlacemarks) {
                assertEquals(getFeatureSource(MockData.BASIC_POLYGONS).getFeatures().size(),
                        document.getElementsByTagName("Placemark").getLength());
            } else {
                assertEquals(0, document.getElementsByTagName("Placemark").getLength());
            }

            zipFile.close();
        } finally {
            kmzMap.dispose();
        }
    }

    @Test
    public void testSuperOverlayTransformer() throws Exception {
        KMLSuperOverlayTransformer transformer = new KMLSuperOverlayTransformer(getWMS(),
                mapContent);
        transformer.setIndentation(2);

        mapContent.getViewport().setBounds(new ReferencedEnvelope(-180.0, 180.0, -90.0, 90.0,
                DefaultGeographicCRS.WGS84));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(layer, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(5, document.getElementsByTagName("Region").getLength());
        assertEquals(4, document.getElementsByTagName("NetworkLink").getLength());
    }

    @Test
    public void testStyleConverter() throws Exception {
        KMLTransformer transformer = new KMLTransformer(getWMS());
        mapContent.removeLayer(mapContent.layers().get(0));
        mapContent.addLayer(createMapLayer(MockData.BASIC_POLYGONS, "allsymbolizers"));
        mapContent.getViewport().setBounds(new ReferencedEnvelope(-180, 0, -90, 90,
                DefaultGeographicCRS.WGS84));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        Document document = WMSTestSupport.transform(mapContent, transformer, false);
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(3, document.getElementsByTagName("Style").getLength());
        XMLAssert.assertXpathEvaluatesTo("0", "count(//Style[1]/IconStyle/Icon/color)", document);
        XMLAssert.assertXpathEvaluatesTo("http://localhost:8080/geoserver/kml/icon/allsymbolizers?0.0.0=",
                "//Style[1]/IconStyle/Icon/href", document);
        XMLAssert.assertXpathEvaluatesTo("b24d4dff", "//Style[1]/PolyStyle/color", document);
        XMLAssert.assertXpathEvaluatesTo("1", "//Style[1]/PolyStyle/outline", document);
        XMLAssert.assertXpathEvaluatesTo("ffba3e00", "//Style[1]/LineStyle/color", document);
    }

    /**
     * See http://jira.codehaus.org/browse/GEOS-2670
     */
    @Test
    public void testDynamicSymbolizer() throws Exception {
        KMLTransformer transformer = new KMLTransformer(getWMS());
        mapContent.removeLayer(mapContent.layers().get(0));
        mapContent.addLayer(createMapLayer(MockData.STREAMS, "dynamicsymbolizer"));
        mapContent.getViewport().setBounds(new ReferencedEnvelope(-180, 0, -90, 90,
                DefaultGeographicCRS.WGS84));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        Document document = WMSTestSupport.transform(mapContent, transformer, false);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(1, document.getElementsByTagName("Style").getLength());
        XMLAssert.assertXpathEvaluatesTo("http://example.com/Cam Stream",
                "//Style[1]/IconStyle/Icon/href", document);
    }
    
    @Test
    public void testRelativeDynamicSymbolizer() throws Exception {
        KMLTransformer transformer = new KMLTransformer(getWMS());
        mapContent.removeLayer(mapContent.layers().get(0));
        mapContent.addLayer(createMapLayer(MockData.STREAMS, "relativeds"));
        mapContent.getViewport().setBounds(new ReferencedEnvelope(-180, 0, -90, 90,
                DefaultGeographicCRS.WGS84));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        Document document = WMSTestSupport.transform(mapContent, transformer, false);
        
        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(1, document.getElementsByTagName("Style").getLength());
        XMLAssert.assertXpathEvaluatesTo("http://localhost:8080/geoserver/styles/icons/Cam%20Stream",
                "//Style[1]/IconStyle/Icon/href", document);
    }
    
    @Test
    public void testExternalImageSize() throws Exception {
        KMLTransformer transformer = new KMLTransformer(getWMS());
        mapContent.removeLayer(mapContent.layers().get(0));
        mapContent.addLayer(createMapLayer(MockData.STREAMS, "big-local-image"));
        mapContent.getViewport().setBounds(new ReferencedEnvelope(-180, 0, -90, 90,
                DefaultGeographicCRS.WGS84));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);
        
        Document document = WMSTestSupport.transform(mapContent, transformer, false);
        print(document);
        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(1, document.getElementsByTagName("Style").getLength());
        XPath xPath = XPathFactory.newInstance().newXPath();
        Double scale = (Double)xPath.evaluate("//Style[1]/IconStyle/scale",
                document.getDocumentElement(), XPathConstants.NUMBER);
        assertThat(scale, closeTo(42d/16d, 0.01));
    }

    @Test
    public void testTransformer() throws Exception {
        KMLTransformer transformer = new KMLTransformer(getWMS());

        Document document = WMSTestSupport.transform(mapContent, transformer);

        assertEquals("kml", document.getDocumentElement().getNodeName());
    }
}
