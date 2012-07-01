/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.kml.KMZMapResponse.KMZMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class KMLTransformerTest extends WMSTestSupport {
    WMSMapContext mapContext;

    MapLayer mapLayer;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new KMLTransformerTest());
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        mapLayer = createMapLayer(MockData.BASIC_POLYGONS);

        mapContext = new WMSMapContext(createGetMapRequest(MockData.BASIC_POLYGONS));
        mapContext.addLayer(mapLayer);
    }

    @Override
    protected void tearDownInternal() {
        mapContext.dispose();
        // mapContext.clearLayerList();
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("allsymbolizers", getClass().getResource("allsymbolizers.sld"));
        dataDirectory.addStyle("SingleFeature", getClass().getResource("singlefeature.sld"));
        dataDirectory.addStyle("Bridge", getClass().getResource("bridge.sld"));
        dataDirectory.addStyle("BridgeSubdir", getClass().getResource("bridgesubdir.sld"));
        dataDirectory
                .addStyle("dynamicsymbolizer", getClass().getResource("dynamicsymbolizer.sld"));
        dataDirectory
                 .addStyle("relativeds", getClass().getResource("relativeds.sld"));
        dataDirectory.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/bridge.png");
        new File(dataDirectory.getDataDirectoryRoot(), "styles/graphics").mkdir();
        dataDirectory.copyTo(getClass().getResourceAsStream("bridge.png"),
                "styles/graphics/bridgesubdir.png");
    }

    public void testVectorTransformer() throws Exception {
        KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContext, mapLayer);
        transformer.setIndentation(2);

        SimpleFeatureSource featureSource = DataUtilities.simple((FeatureSource) mapLayer
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
    public void testExternalGraphicBackround() throws Exception {

        MapLayer mapLayer = createMapLayer(MockData.POINTS, "Bridge");
        Document document;

        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (SimpleFeatureSource) mapLayer.getFeatureSource();
        int nfeatures = featureSource.getFeatures().size();
        WMSMapContext mapContext = new WMSMapContext(createGetMapRequest(MockData.POINTS));
        try {
            mapContext.addLayer(mapLayer);
            KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContext,
                    mapLayer);
            transformer.setIndentation(2);

            // print(document);
            document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);
        } finally {
            mapContext.dispose();
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
    public void testExternalGraphicSubdir() throws Exception {

        MapLayer mapLayer = createMapLayer(MockData.POINTS, "BridgeSubdir");

        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (SimpleFeatureSource) mapLayer.getFeatureSource();

        WMSMapContext mapContext = new WMSMapContext(createGetMapRequest(MockData.POINTS));
        Document document;
        try {
            mapContext.addLayer(mapLayer);
            KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContext,
                    mapLayer);
            transformer.setIndentation(2);
            // print(document);
            document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);
        } finally {
            mapContext.dispose();
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
    public void testProxyBaseURL() throws Exception {
        GeoServer gs = getGeoServer();
        try {
            GeoServerInfo info = gs.getGlobal();
            info.setProxyBaseUrl("http://myhost:9999/gs");
            gs.save(info);

            MapLayer mapLayer = createMapLayer(MockData.POINTS, "Bridge");
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
            featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) mapLayer
                    .getFeatureSource();
            int nfeatures = featureSource.getFeatures().size();

            WMSMapContext mapContext = new WMSMapContext(createGetMapRequest(MockData.POINTS));
            Document document;

            try {
                mapContext.addLayer(mapLayer);
                KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContext,
                        mapLayer);
                transformer.setIndentation(2);

                document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);
                // print(document);
            } finally {
                mapContext.dispose();
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

    public void testFilteredData() throws Exception {
        MapLayer mapLayer = createMapLayer(MockData.BASIC_POLYGONS, "SingleFeature");

        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = (SimpleFeatureSource) mapLayer.getFeatureSource();

        WMSMapContext mapContext = new WMSMapContext(createGetMapRequest(MockData.BASIC_POLYGONS));
        Document document;
        try {
            mapContext.addLayer(mapLayer);

            KMLVectorTransformer transformer = new KMLVectorTransformer(getWMS(), mapContext,
                    mapLayer);
            transformer.setIndentation(2);
            document = WMSTestSupport.transform(featureSource.getFeatures(), transformer);
        } finally {
            mapContext.dispose();
        }
        Element element = document.getDocumentElement();
        assertEquals("kml", element.getNodeName());
        assertEquals(1, element.getElementsByTagName("Placemark").getLength());
        assertEquals(1, element.getElementsByTagName("Style").getLength());
    }

    // public void testReprojection() throws Exception {
    // KMLTransformer transformer = new KMLTransformer();
    // transformer.setIndentation(2);
    //
    // ByteArrayOutputStream output = new ByteArrayOutputStream();
    // transformer.transform(mapContext, output);
    // transformer.transform(mapContext,System.out);
    // DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    // Document doc1 = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
    //
    // mapContext.setCoordinateReferenceSystem(CRS.decode("EPSG:3005"));
    // output = new ByteArrayOutputStream();
    // transformer.transform(mapContext, output);
    // transformer.transform(mapContext,System.out);
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

    public void testRasterTransformerInline() throws Exception {
        KMLRasterTransformer transformer = new KMLRasterTransformer(getWMS(), mapContext);
        transformer.setInline(true);

        Document document = WMSTestSupport.transform(mapLayer, transformer);

        assertEquals("kml", document.getDocumentElement().getNodeName());

        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("Folder")
                .getLength());
        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("GroundOverlay")
                .getLength());

        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("href").getLength());

        Element href = (Element) document.getElementsByTagName("href").item(0);
        assertEquals("images/layer_0.png", href.getFirstChild().getNodeValue());
    }

    public void testRasterTransformerNotInline() throws Exception {
        KMLRasterTransformer transformer = new KMLRasterTransformer(getWMS(), mapContext);
        transformer.setInline(false);

        Document document = WMSTestSupport.transform(mapLayer, transformer);

        assertEquals("kml", document.getDocumentElement().getNodeName());

        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("Folder")
                .getLength());
        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("GroundOverlay")
                .getLength());

        assertEquals(mapContext.getLayerCount(), document.getElementsByTagName("href").getLength());

        Element href = (Element) document.getElementsByTagName("href").item(0);
        assertTrue(href.getFirstChild().getNodeValue().startsWith("http://localhost"));
    }

    public void testRasterPlacemarkTrue() throws Exception {
        doTestRasterPlacemark(true);
    }

    public void testRasterPlacemarkFalse() throws Exception {
        doTestRasterPlacemark(false);
    }

    protected void doTestRasterPlacemark(boolean doPlacemarks) throws Exception {
        GetMapRequest getMapRequest = createGetMapRequest(MockData.BASIC_POLYGONS);
        HashMap formatOptions = new HashMap();
        formatOptions.put("kmplacemark", new Boolean(doPlacemarks));
        formatOptions.put("kmscore", new Integer(0));
        getMapRequest.setFormatOptions(formatOptions);

        WMSMapContext mapContext = new WMSMapContext(getMapRequest);
        mapContext.addLayer(mapLayer);
        mapContext.setMapHeight(1024);
        mapContext.setMapWidth(1024);

        // create the map producer
        KMZMapOutputFormat mapProducer = new KMZMapOutputFormat(getWMS());
        KMZMapResponse mapEncoder = new KMZMapResponse(getWMS());
        KMZMap kmzMap = mapProducer.produceMap(mapContext);
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

    public void testSuperOverlayTransformer() throws Exception {
        KMLSuperOverlayTransformer transformer = new KMLSuperOverlayTransformer(getWMS(),
                mapContext);
        transformer.setIndentation(2);

        mapContext.setAreaOfInterest(new ReferencedEnvelope(-180.0, 180.0, -90.0, 90.0,
                DefaultGeographicCRS.WGS84));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(mapLayer, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(5, document.getElementsByTagName("Region").getLength());
        assertEquals(4, document.getElementsByTagName("NetworkLink").getLength());
    }

    public void testStyleConverter() throws Exception {
        KMLTransformer transformer = new KMLTransformer(getWMS());
        mapContext.removeLayer(mapContext.getLayer(0));
        mapContext.addLayer(createMapLayer(MockData.BASIC_POLYGONS, "allsymbolizers"));
        mapContext.setAreaOfInterest(new ReferencedEnvelope(-180, 0, -90, 90,
                DefaultGeographicCRS.WGS84));
        mapContext.setMapHeight(256);
        mapContext.setMapWidth(256);

        Document document = WMSTestSupport.transform(mapContext, transformer, false);
        // print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(3, document.getElementsByTagName("Style").getLength());
        XMLAssert.assertXpathEvaluatesTo("0", "count(//Style[1]/IconStyle/Icon/color)", document);
        XMLAssert.assertXpathEvaluatesTo("http://maps.google.com/mapfiles/kml/pal4/icon25.png",
                "//Style[1]/IconStyle/Icon/href", document);
        XMLAssert.assertXpathEvaluatesTo("b24d4dff", "//Style[1]/PolyStyle/color", document);
        XMLAssert.assertXpathEvaluatesTo("1", "//Style[1]/PolyStyle/outline", document);
        XMLAssert.assertXpathEvaluatesTo("ffba3e00", "//Style[1]/LineStyle/color", document);
    }

    /**
     * See http://jira.codehaus.org/browse/GEOS-2670
     */
    public void testDynamicSymbolizer() throws Exception {
        KMLTransformer transformer = new KMLTransformer(getWMS());
        mapContext.removeLayer(mapContext.getLayer(0));
        mapContext.addLayer(createMapLayer(MockData.STREAMS, "dynamicsymbolizer"));
        mapContext.setAreaOfInterest(new ReferencedEnvelope(-180, 0, -90, 90,
                DefaultGeographicCRS.WGS84));
        mapContext.setMapHeight(256);
        mapContext.setMapWidth(256);

        Document document = WMSTestSupport.transform(mapContext, transformer, false);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(1, document.getElementsByTagName("Style").getLength());
        XMLAssert.assertXpathEvaluatesTo("http://example.com/Cam Stream",
                "//Style[1]/IconStyle/Icon/href", document);
    }
    
    public void testRelativeDynamicSymbolizer() throws Exception {
        KMLTransformer transformer = new KMLTransformer(getWMS());
        mapContext.removeLayer(mapContext.layers().get(0));
        mapContext.addLayer(createMapLayer(MockData.STREAMS, "relativeds"));
        mapContext.getViewport().setBounds(new ReferencedEnvelope(-180, 0, -90, 90,
                DefaultGeographicCRS.WGS84));
        mapContext.setMapHeight(256);
        mapContext.setMapWidth(256);

        Document document = WMSTestSupport.transform(mapContext, transformer, false);
        
        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(1, document.getElementsByTagName("Style").getLength());
        XMLAssert.assertXpathEvaluatesTo("http://localhost:8080/geoserver/styles/icons/Cam%20Stream",
                "//Style[1]/IconStyle/Icon/href", document);
    }

    public void testTransformer() throws Exception {
        KMLTransformer transformer = new KMLTransformer(getWMS());

        Document document = WMSTestSupport.transform(mapContext, transformer);

        assertEquals("kml", document.getDocumentElement().getNodeName());
    }
}
